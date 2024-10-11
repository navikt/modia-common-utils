package no.nav.personoversikt.common.typeanalyzer

import no.nav.personoversikt.common.test.snapshot.SnapshotExtension
import no.nav.personoversikt.common.test.snapshot.format.JsonSnapshotFormat
import no.nav.personoversikt.common.test.snapshot.format.TextSnapshotFormat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.random.Random

internal class TypeanalyzerSnapshotTest {
    @JvmField
    @RegisterExtension
    val jsonSnapshot = SnapshotExtension(format = JsonSnapshotFormat.plain)

    @JvmField
    @RegisterExtension
    val textSnapshot = SnapshotExtension(format = TextSnapshotFormat)

    @Test
    internal fun `should create be able to create simple analysis`() {
        val analyzer = Typeanalyzer()
        val capture = analyzer.capture(valueObject)

        assertEquals(capture, analyzer.report())
        jsonSnapshot.assertMatches(capture)
    }

    @Test
    internal fun `should reconcile nullability of fields preexisting fields`() {
        val analyzer = Typeanalyzer()
        val captures =
            arrayOf(
                analyzer.capture(valueObject),
                analyzer.capture(RootObject()),
                analyzer.capture(null),
                analyzer.capture(
                    valueObject.copy(
                        listOfStuff =
                            listOf(
                                Stuff(
                                    id = null,
                                    meta = mapOf("key" to null, "key2" to null),
                                ),
                                null,
                                Stuff(
                                    id = null,
                                    meta = null,
                                ),
                            ),
                    ),
                ),
            )

        assertNotEquals(captures[0], analyzer.report())
        assertEquals(captures.last(), analyzer.report())
        for (capture in captures) {
            jsonSnapshot.assertMatches(capture)
        }
    }

    data class SimpleList(
        val list: List<String?>?,
    )

    @Test
    internal fun `all nullability fields for list construct should be able to be updated`() {
        val analyzer = Typeanalyzer()
        val captures =
            arrayOf(
                analyzer.capture(SimpleList(emptyList())),
                analyzer.capture(SimpleList(listOf("hei"))),
                analyzer.capture(SimpleList(listOf("hei", null, "other"))),
                analyzer.capture(SimpleList(null)),
                analyzer.capture(null),
            )

        assertNotEquals(captures[0], analyzer.report())
        assertEquals(captures.last(), analyzer.report())
        for (capture in captures) {
            jsonSnapshot.assertMatches(capture)
        }
    }

    @Test
    internal fun `key mismatch makes elements nullable`() {
        val analyzer = Typeanalyzer()
        val captures =
            arrayOf(
                analyzer.capture(mapOf("key1" to "value")),
                analyzer.capture(mapOf("key2" to "value")),
            )

        assertNotEquals(captures[0], analyzer.report())
        assertEquals(captures.last(), analyzer.report())
        for (capture in captures) {
            jsonSnapshot.assertMatches(capture)
        }
    }

    @Test
    internal fun `should override types even if types mismatch`() {
        val analyzer = Typeanalyzer()
        val captures =
            arrayOf(
                analyzer.capture(valueObject),
                analyzer.capture(
                    mapOf(
                        "active" to true,
                        "count" to null,
                        "countLong" to null,
                        "fraction" to null,
                    ),
                ),
            )

        assertNotEquals(captures[0], analyzer.report())
        assertEquals(captures.last(), analyzer.report())
        for (capture in captures) {
            jsonSnapshot.assertMatches(capture)
        }
    }

    @Test
    internal fun `should not throw error`() {
        val analyzer = Typeanalyzer()

        analyzer.capture(valueObject)
        val capture = analyzer.capture("an primitive value")

        jsonSnapshot.assertMatches(capture)
    }

    @Test
    internal fun `pretty print`() {
        val analyzer = Typeanalyzer()
        val rnd = Random(0)
        val capture = requireNotNull(analyzer.capture(valueObject))

        textSnapshot.assertMatches(Formatter(KotlinFormat, rnd).print(capture))
        textSnapshot.assertMatches(Formatter(TypescriptFormat, rnd).print(capture))
    }
}
