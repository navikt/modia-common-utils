package no.nav.personoversikt.typeanalyzer

import no.nav.personoversikt.common.test.snapshot.SnapshotExtension
import no.nav.personoversikt.common.test.snapshot.format.TextSnapshotFormat
import no.nav.personoversikt.typeanalyzer.TypeanalyzerTest.CaptureAsserter.Companion.assertCapture
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class TypeanalyzerTest {
    @JvmField
    @RegisterExtension
    val textSnapshot = SnapshotExtension(format = TextSnapshotFormat)

    @Test
    internal fun `should be able to handle primitive values`() {
        assertCapture(Typeanalyzer().capture(null))
            .hasType<NullCapture>()

        assertCapture(Typeanalyzer().capture(1))
            .hasType<PrimitiveCapture>()
            .assertPrimitive(CaptureType.INT, nullable = false)

        assertCapture(Typeanalyzer().capture(2.toLong()))
            .hasType<PrimitiveCapture>()
            .assertPrimitive(CaptureType.INT, nullable = false)

        assertCapture(Typeanalyzer().capture(0.3))
            .hasType<PrimitiveCapture>()
            .assertPrimitive(CaptureType.DOUBLE, nullable = false)

        assertCapture(Typeanalyzer().capture(0.4f))
            .hasType<PrimitiveCapture>()
            .assertPrimitive(CaptureType.DOUBLE, nullable = false)

        assertCapture(Typeanalyzer().capture("text"))
            .hasType<PrimitiveCapture>()
            .assertPrimitive(CaptureType.TEXT, nullable = false)
    }

    @Test
    internal fun `should be able to handle list values`() {
        assertCapture(Typeanalyzer().capture(listOf("value", "values")))
            .hasType<ListCapture>()
            .assertList(listNullable = false, listElementNullable = false)
    }

    @Test
    internal fun `should be able to handle map values`() {
        assertCapture(Typeanalyzer().capture(mapOf("key" to listOf("values"))))
            .hasType<ObjectCapture>()
            .assertObject(objectNullable = false, listNullable = false, listElementNullable = false)
    }

    @Test
    internal fun `should relax nullability for primitives`() {
        val analyzer = Typeanalyzer()
        analyzer.capture(1)

        assertCapture(analyzer.capture(null))
            .hasType<PrimitiveCapture>()
            .assertPrimitive(CaptureType.INT, nullable = true)
    }

    @Test
    internal fun `should relax nullability for list`() {
        val analyzer = Typeanalyzer()
        analyzer.capture(listOf("value", "values"))

        assertCapture(analyzer.capture(listOf(null)))
            .hasType<ListCapture>()
            .assertList(listNullable = false, listElementNullable = true)

        assertCapture(analyzer.capture(null))
            .hasType<ListCapture>()
            .assertList(listNullable = true, listElementNullable = true)
    }

    @Test
    internal fun `should relax nullability for object`() {
        val analyzer = Typeanalyzer()
        analyzer.capture(mapOf("key" to listOf("values")))

        assertCapture(analyzer.capture(mapOf("key" to listOf(null))))
            .hasType<ObjectCapture>()
            .assertObject(objectNullable = false, listNullable = false, listElementNullable = true)

        assertCapture(analyzer.capture(mapOf("key" to null)))
            .hasType<ObjectCapture>()
            .assertObject(objectNullable = false, listNullable = true, listElementNullable = true)

        assertCapture(analyzer.capture(null))
            .hasType<ObjectCapture>()
            .assertObject(objectNullable = true, listNullable = true, listElementNullable = true)
    }

    @Test
    internal fun `should specify type for null values`() {
        val analyzer = Typeanalyzer()
        assertCapture(analyzer.capture(null))
            .hasType<NullCapture>()

        assertCapture(analyzer.capture("text"))
            .hasType<PrimitiveCapture>()
            .assertPrimitive(CaptureType.TEXT, true)
    }

    @Test
    internal fun `should specify type in lists for null values`() {
        val analyzer = Typeanalyzer()
        assertCapture(analyzer.capture(listOf(null)))
            .hasType<ListCapture>()
            .assert { base ->
                assertEquals(CaptureType.LIST, base.value.type)
                assertEquals(false, base.value.nullable)

                assertCapture(base.value.subtype)
                    .hasType<NullCapture>()
            }

        assertCapture(analyzer.capture(listOf("test")))
            .hasType<ListCapture>()
            .assertList(listNullable = false, listElementNullable = true)
            .assert {
                assertCapture(it.value.subtype)
                    .hasType<PrimitiveCapture>()
                    .assertPrimitive(CaptureType.TEXT, true)
            }
    }

    @Test
    internal fun `should specify type in lists for unknown values`() {
        val analyzer = Typeanalyzer()
        assertCapture(analyzer.capture(emptyList<String>()))
            .hasType<ListCapture>()
            .assert { base ->
                assertEquals(CaptureType.LIST, base.value.type)
                assertEquals(false, base.value.nullable)

                assertCapture(base.value.subtype)
                    .hasType<UnknownCapture>()
            }

        assertCapture(analyzer.capture(listOf("test")))
            .hasType<ListCapture>()
            .assertList(listNullable = false, listElementNullable = false)
            .assert {
                assertCapture(it.value.subtype)
                    .hasType<PrimitiveCapture>()
                    .assertPrimitive(CaptureType.TEXT, false)
            }
    }

    @Test
    internal fun `should specify type in objects for null values`() {
        val analyzer = Typeanalyzer()
        assertCapture(analyzer.capture(mapOf("key" to null)))
            .hasType<ObjectCapture>()
            .assert {
                assertCapture(it.value.getField("key"))
                    .hasType<NullCapture>()
            }

        assertCapture(analyzer.capture(mapOf("key" to "text")))
            .hasType<ObjectCapture>()
            .assert {
                assertCapture(it.value.getField("key"))
                    .hasType<PrimitiveCapture>()
                    .assertPrimitive(CaptureType.TEXT, nullable = true)
            }
    }

    @Test
    internal fun `should specify type in objects for unknown values`() {
        val analyzer = Typeanalyzer()
        assertCapture(analyzer.capture(emptyMap<String, Any>()))
            .hasType<ObjectCapture>()
            .assert {
                assertTrue(it.value.fields.isEmpty())
            }

        assertCapture(analyzer.capture(mapOf("key" to "text")))
            .hasType<ObjectCapture>()
            .assert {
                assertCapture(it.value.getField("key"))
                    .hasType<PrimitiveCapture>()
                    .assertPrimitive(CaptureType.TEXT, nullable = true)
            }
    }

    @Test
    internal fun `should relax nullability when map updates`() {
        val analyzer = Typeanalyzer()
        assertCapture(analyzer.capture(mapOf("key1" to "value1")))
            .hasType<ObjectCapture>()
            .assert {
                assertCapture(it.value.getField("key1"))
                    .hasType<PrimitiveCapture>()
                    .assertPrimitive(CaptureType.TEXT, nullable = false)
            }

        assertCapture(analyzer.capture(mapOf("key2" to "value2")))
            .hasType<ObjectCapture>()
            .assert {
                assertCapture(it.value.getField("key1"))
                    .hasType<PrimitiveCapture>()
                    .assertPrimitive(CaptureType.TEXT, nullable = true)
                assertCapture(it.value.getField("key2"))
                    .hasType<PrimitiveCapture>()
                    .assertPrimitive(CaptureType.TEXT, nullable = true)
            }
    }

    private fun CaptureAsserter<ObjectCapture>.assertObject(
        objectNullable: Boolean,
        listNullable: Boolean,
        listElementNullable: Boolean
    ): CaptureAsserter<ObjectCapture> {
        this.assert { base ->
            assertEquals(CaptureType.OBJECT, base.value.type)
            assertEquals(objectNullable, base.value.nullable)

            assertCapture(base.value.getField("key"))
                .hasType<ListCapture>()
                .assertList(listNullable, listElementNullable)
        }
        return this
    }

    private fun CaptureAsserter<ListCapture>.assertList(
        listNullable: Boolean,
        listElementNullable: Boolean
    ): CaptureAsserter<ListCapture> {
        this.assert { base ->
            assertEquals(CaptureType.LIST, base.value.type)
            assertEquals(listNullable, base.value.nullable)

            assertCapture(base.value.subtype)
                .hasType<PrimitiveCapture>()
                .assertPrimitive(CaptureType.TEXT, listElementNullable)
        }
        return this
    }

    private fun CaptureAsserter<PrimitiveCapture>.assertPrimitive(
        type: CaptureType,
        nullable: Boolean
    ): CaptureAsserter<PrimitiveCapture> {
        this.assert {
            assertEquals(type, it.value.type)
            assertEquals(nullable, it.value.nullable)
        }
        return this
    }

    private fun ObjectCapture.getField(name: String): Capture {
        return this.fields.getOrElse(name) { error("Key not found") }
    }

    class CaptureAsserter<TYPE : Capture>(val value: TYPE) {
        companion object {
            fun assertCapture(value: Any?): CaptureAsserter<*> {
                assertNotNull(value) { "Value cannot be null" }
                assertTrue(value is Capture) { "Value must be a capture type" }
                return CaptureAsserter(value as Capture)
            }
        }

        inline fun <reified NEWTYPE : Capture> hasType(): CaptureAsserter<NEWTYPE> {
            assertTrue(value is NEWTYPE) {
                """
                    Expected value to be of ${NEWTYPE::class.simpleName} but was ${value::class.simpleName}
                    $value
                """.trimIndent()
            }
            return CaptureAsserter(value as NEWTYPE)
        }

        fun assert(block: (CaptureAsserter<TYPE>) -> Unit): CaptureAsserter<TYPE> {
            block(assertCapture(value) as CaptureAsserter<TYPE>)
            return this
        }
    }
}
