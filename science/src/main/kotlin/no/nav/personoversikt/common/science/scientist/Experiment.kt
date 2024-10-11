package no.nav.personoversikt.common.science.scientist

import com.fasterxml.jackson.databind.JsonNode
import no.nav.personoversikt.common.science.Rate
import no.nav.personoversikt.common.utils.ConcurrencyUtils.inParallel

class Experiment<T> internal constructor(
    private val config: Scientist.Config,
) {
    fun run(
        control: () -> T,
        experiment: () -> Any?,
        fields: ((Scientist.Markers, T?, Result<Any?>) -> Unit)? = null,
        rate: Rate? = null,
    ): T = runWithResult(control, experiment, fields, rate).control

    fun runWithResult(
        control: () -> T,
        experiment: () -> Any?,
        fields: ((Scientist.Markers, T?, Result<Any?>) -> Unit)? = null,
        rate: Rate? = null,
    ): Scientist.Result<T> {
        val shouldRunExperiment = Scientist.forceExperiment.get() == true || ((rate ?: config.rate).evaluate())
        if (!shouldRunExperiment) {
            return Scientist.Result(
                experimentRun = false,
                control = control(),
            )
        }

        val markers = Scientist.Markers()
        val controlSwappable = config.threadSwappingFn(control) as () -> T
        val experimentSwappable = config.threadSwappingFn(experiment)
        val (controlResult, experimentResult) =
            inParallel(
                first = {
                    measureTimeInMillies {
                        runCatching(controlSwappable)
                    }
                },
                second = {
                    measureTimeInMillies {
                        runCatching(experimentSwappable)
                    }
                },
            )

        markers.fieldAndTag("controlTime", controlResult.time)
        markers.fieldAndTag("experimentTime", experimentResult.time)
        fields?.invoke(markers, controlResult.value.getOrNull(), experimentResult.value)

        if (experimentResult.value.isFailure) {
            markers.fieldAndTag("ok", false)
            markers.field("exception", experimentResult.value.exceptionOrNull()?.message)
            if (config.logAndCompareValues) {
                markers.field("control", controlResult.value.getOrNull())
            }
        } else if (config.logAndCompareValues) {
            val (ok, controlJson, experimentJson) =
                compareAndSerialize(
                    control = controlResult.value.getOrNull(),
                    experiment = experimentResult.value.getOrThrow(),
                )
            markers.fieldAndTag("ok", ok)
            markers.field("control", controlJson)
            markers.field("experiment", experimentJson)
        } else {
            markers.fieldAndTag("ok", true)
        }

        config.reporter(
            "[SCIENCE] ${config.name}",
            markers.fields,
            markers.tags,
            experimentResult.value.exceptionOrNull(),
        )

        return Scientist.Result(
            experimentRun = true,
            control = controlResult.value.getOrThrow(),
            experiment = experimentResult.value.getOrNull(),
            exception = experimentResult.value.exceptionOrNull(),
        )
    }

    companion object {
        private fun compareAndSerialize(
            control: Any?,
            experiment: Any?,
        ): Triple<Boolean, String, String> {
            val (controlJson, controlTree) = process(control)
            val (experimentJson, experimentTree) = process(experiment)
            return Triple(
                controlTree == experimentTree,
                controlJson,
                experimentJson,
            )
        }

        private fun process(value: Any?): Pair<String, JsonNode> {
            val json = Scientist.objectMapper.writeValueAsString(value)
            val tree = Scientist.objectMapper.readTree(json)
            return Pair(json, tree)
        }

        /**
         * Should be removed once `kotlin.time.measureTimedValue` no longer is experimental
         */
        internal data class TimedValue<T>(
            val value: T,
            val time: Long,
        )

        internal fun <T> measureTimeInMillies(block: () -> T): TimedValue<T> {
            val startTime = System.currentTimeMillis()
            val value = block()
            val time = System.currentTimeMillis() - startTime
            return TimedValue(value, time)
        }
    }
}
