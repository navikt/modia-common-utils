package no.nav.personoversikt.common.science.scientist

import no.nav.personoversikt.common.science.Rate
import no.nav.personoversikt.common.utils.KotlinUtils.plusminus
import no.nav.personoversikt.common.utils.makeTransferable
import no.nav.personoversikt.common.utils.withValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ScientistTest {
    @Test
    internal fun `experiment name should be in report`() {
        Scientist.createExperiment<String>(
            Scientist.Config(
                name = "DummyExperiment",
                rate = { true },
                reporter = { header, fields, tags, _ ->
                    assertTrue(header.startsWith("[SCIENCE] DummyExperiment"))
                }
            )
        ).run({ "Control" }, { "Experiment" })
    }

    @Test
    internal fun `experiment should report errors if results are equal`() {
        Scientist.createExperiment<String>(
            Scientist.Config(
                name = "DummyExperiment",
                rate = { true },
                reporter = { header, fields, tags, _ ->
                    assertMapContains("ok", true, fields)
                    assertMapContains("ok", true, tags)
                    assertTrue(fields.containsKey("control"))
                    assertTrue(fields.containsKey("experiment"))
                }
            )
        ).run({ "Hello, World" }, { "Hello, World" })
    }

    @Test
    internal fun `experiment should report error if results are unequal`() {
        Scientist.createExperiment<String>(
            Scientist.Config(
                name = "DummyExperiment",
                rate = { true },
                reporter = { header, fields, tags, _ ->
                    assertMapContains("ok", false, fields)
                    assertMapContains("ok", false, tags)
                    assertTrue(fields.containsKey("control"))
                    assertTrue(fields.containsKey("controlTime"))
                    assertTrue(fields.containsKey("experiment"))
                    assertTrue(fields.containsKey("experimentTime"))
                    assertTrue(tags.containsKey("controlTime"))
                    assertTrue(tags.containsKey("experimentTime"))
                    assertEquals("\"Hello\"", fields["control"])
                    assertEquals("\"World\"", fields["experiment"])
                }
            )
        ).run({ "Hello" }, { "World" })
    }

    @Test
    internal fun `experiment should respect the experiment rate`() {
        var experimentsRun = 0
        val experiment = Scientist.createExperiment<String>(
            Scientist.Config(
                name = "DummyExperiment",
                rate = Rate.FixedValue(0.7),
                reporter = { _, _, _, _ -> experimentsRun++ }
            )
        )
        repeat(1000) {
            experiment.run({ "Hello" }, { "World" })
        }
        assertWithinRange(700 plusminus 100, experimentsRun)
    }

    @Test
    internal fun `experiment should respect experiment rate override`() {
        var experimentsRun = 0
        val experiment = Scientist.createExperiment<String>(
            Scientist.Config(
                name = "DummyExperiment",
                rate = { true },
                reporter = { _, _, _, _ -> experimentsRun++ }
            )
        )
        repeat(1000) {
            experiment.run(
                control = { "Hello" },
                experiment = { "World" },
                rate = Rate.FixedValue(0.7)
            )
        }

        assertWithinRange(700 plusminus 100, experimentsRun)
    }

    data class DummyObject(val id: String, val code: String)

    @Test
    internal fun `experiment should serialize results and do deep comparision`() {
        val controlResult = listOf(
            DummyObject("123", "DAG"),
            DummyObject("456", "AAP"),
            DummyObject("789", "SYK")
        )
        val experimentResult = listOf(
            DummyObject("123", "DAG"),
            DummyObject("456", "AAP"),
            DummyObject("789", "SYK")
        )
        Scientist.createExperiment<List<DummyObject>>(
            Scientist.Config(
                name = "DummyExperiment",
                rate = { true },
                reporter = { header, fields, tags, _ ->
                    assertMapContains("ok", true, fields)
                    assertMapContains("ok", true, tags)
                    assertTrue(fields.containsKey("control"))
                    assertTrue(fields.containsKey("controlTime"))
                    assertTrue(tags.containsKey("controlTime"))

                    assertTrue(fields.containsKey("experiment"))
                    assertTrue(fields.containsKey("experimentTime"))
                    assertTrue(tags.containsKey("experimentTime"))
                }
            )
        ).run({ controlResult }, { experimentResult })
    }

    private data class Dataholder(val id: String, val count: Int, val isDone: Boolean)

    @Test
    internal fun `experiment should compare untyped maps to typed objects correctly`() {
        val controlResult = listOf(
            mapOf(
                "id" to "123",
                "count" to 4,
                "isDone" to false
            ),
            mapOf(
                "id" to "126",
                "count" to 6,
                "isDone" to true
            )
        )
        val experimentResult = listOf(
            Dataholder("123", 4, false),
            Dataholder("126", 6, true)
        )
        Scientist.createExperiment<List<Map<String, Any?>>>(
            Scientist.Config(
                name = "DummyExperiment",
                rate = { true },
                reporter = { header, fields, tags, _ ->
                    assertMapContains("ok", true, fields)
                    assertMapContains("ok", true, tags)

                    assertTrue(fields.containsKey("control"))
                    assertTrue(fields.containsKey("controlTime"))
                    assertTrue(tags.containsKey("controlTime"))

                    assertTrue(fields.containsKey("experiment"))
                    assertTrue(fields.containsKey("experimentTime"))
                    assertTrue(tags.containsKey("experimentTime"))
                }
            )
        ).run({ controlResult }, { experimentResult })
    }

    @Test
    internal fun `should report metadatafields`() {
        Scientist.createExperiment<String>(
            Scientist.Config(
                name = "DummyExperiment",
                rate = { true },
                reporter = { header, fields, tags, _ ->
                    assertMapContains("ok", true, fields)
                    assertMapContains("ok", true, tags)
                    assertTrue(fields.containsKey("control"))
                    assertTrue(fields.containsKey("experiment"))
                    assertTrue(fields.containsKey("control-extra"))
                    assertTrue(fields.containsKey("experiment-extra"))
                    assertTrue(tags.containsKey("tag-extra"))
                }
            )
        ).runWithResult(
            control = { "Hello, World" },
            experiment = { "Hello, World" },
            fields = { markers, control, triedExperiment ->
                markers.field("control-extra", 1)
                markers.field("experiment-extra", "value")
                markers.tag("tag-extra", "value")
            }
        )
    }

    @Test
    internal fun `should run experiment in parallel`() {
        val startTime = System.currentTimeMillis()
        Scientist.createExperiment<String>(
            Scientist.Config(
                name = "DummyExperiment",
                rate = { true },
                reporter = { _, _, _, _ ->
                    val endTime = System.currentTimeMillis()
                    assertWithinRange(2000 plusminus 300, (endTime - startTime).toInt())
                }
            )
        ).run(
            control = {
                Thread.sleep(2000L)
                "Control"
            },
            experiment = {
                Thread.sleep(1500L)
                "Experiment"
            }
        )
    }

    @Test
    fun `experiment should utilize threadSwappingFn to make threadlocals`() {
        threadlocal.withValue("main") {
            Scientist.createExperiment<String>(
                Scientist.Config(
                    name = "DummyExperiment",
                    rate = { true },
                    reporter = { _, fields, _, _ ->
                        assertMapContains("control", "\"main\"", fields)
                        assertMapContains("experiment", "\"main\"", fields)
                    },
                    threadSwappingFn = threadlocal::makeTransferable
                )
            ).run(
                control = { threadlocal.get() },
                experiment = { threadlocal.get() },
            )
        }
    }
    private val threadlocal = ThreadLocal<String>()

    @Test
    internal fun `experiment should report even if control throws exception`() {
        assertThrows<IllegalStateException> {
            Scientist.createExperiment<String>(
                Scientist.Config(
                    name = "DummyExperiment",
                    rate = { true },
                    reporter = { header, fields, tags, _ ->
                        assertMapContains("ok", false, fields)
                        assertMapContains("ok", false, tags)
                        assertTrue(fields.containsKey("control"))
                        assertTrue(fields.containsKey("experiment"))
                    }
                )
            ).run({ error("Control has error") }, { "Hello, World" })
        }
    }

    private fun <S, T> assertMapContains(key: S, value: T, map: Map<S, T>) {
        assertTrue(map.containsKey(key), "Map must contain key")
        assertEquals(value, map[key])
    }

    private fun assertWithinRange(range: IntRange, value: Int) {
        assertTrue(range.contains(value), "$range did not contain $value")
    }
}
