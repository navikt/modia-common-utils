package no.nav.personoversikt.common.science.scientist

import com.fasterxml.jackson.databind.json.JsonMapper
import no.nav.personoversikt.common.logging.TjenestekallLogg
import no.nav.personoversikt.common.science.Rate

private typealias Reporter = (header: String, fields: Map<String, Any?>, tags: Map<String, Any?>, exception: Throwable?) -> Unit
object Scientist {
    internal val forceExperiment: ThreadLocal<Boolean?> = ThreadLocal()
    internal val objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build()
    private val logger = TjenestekallLogg.withLogType("scientist")
    private val defaultReporter: Reporter =
        { header, fields, tags, throwable -> logger.info(header, fields, tags, throwable) }

    data class Config(
        val name: String,
        val rate: Rate,
        val reporter: Reporter = defaultReporter,
        val logAndCompareValues: Boolean = true
    )

    data class Result<T>(
        val experimentRun: Boolean,
        val control: T,
        val experiment: Any? = null,
        val exception: Throwable? = null
    )

    class Markers {
        val fields = mutableMapOf<String, Any?>()
        val tags = mutableMapOf<String, Any?>()

        fun field(name: String, value: Any?) {
            fields[name] = value
        }

        fun tag(name: String, value: Any?) {
            tags[name] = value
        }

        fun fieldAndTag(name: String, value: Any?) {
            fields[name] = value
            tags[name] = value
        }
    }

    fun <T : Any?> createExperiment(config: Config) = Experiment<T>(config)
}
