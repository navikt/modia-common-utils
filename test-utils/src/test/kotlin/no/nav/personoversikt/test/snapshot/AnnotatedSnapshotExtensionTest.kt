package no.nav.personoversikt.test.snapshot

import no.nav.personoversikt.test.snapshot.format.JsonSnapshotFormat
import no.nav.personoversikt.test.snapshot.format.TextSnapshotFormat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import java.util.*

object PlainJsonSnapshowExtension : SnapshotExtension(format = JsonSnapshotFormat.plain) {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == PlainJsonSnapshowExtension::class.java
    }
}
object TextSnapshowExtension : SnapshotExtension(format = TextSnapshotFormat) {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == TextSnapshowExtension::class.java
    }
}

@ExtendWith(SnapshotExtension::class)
@ExtendWith(PlainJsonSnapshowExtension::class)
@ExtendWith(TextSnapshowExtension::class)
internal class AnnotatedSnapshotExtensionTest(
    val typedJson: SnapshotExtension,
    val plainJson: PlainJsonSnapshowExtension,
    val text: TextSnapshowExtension,
) {
    data class DummyObject(
        val id: UUID = UUID.fromString("ab76e36b-5001-4716-b5d6-d0c0cf95a412"),
        val name: String = "myname",
        val list: List<Int> = listOf(1, 2, 3)
    )

    @Test
    internal fun `should write typed snapshots`() {
        typedJson.assertMatches(DummyObject())
    }

    @Test
    internal fun `should write plain snapshots`() {
        plainJson.assertMatches(DummyObject())
    }

    @Test
    internal fun `should write text snapshots`() {
        text.assertMatches(DummyObject())
    }
}
