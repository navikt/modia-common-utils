package no.nav.personoversikt.common.test.snapshot

import no.nav.personoversikt.common.test.snapshot.format.JsonSnapshotFormat
import no.nav.personoversikt.common.test.snapshot.format.TextSnapshotFormat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.UUID

internal class SnapshotExtensionTest {
    @JvmField
    @RegisterExtension
    val typedJson = SnapshotExtension(format = JsonSnapshotFormat)

    @JvmField
    @RegisterExtension
    val plainJson = SnapshotExtension(format = JsonSnapshotFormat.plain)

    @JvmField
    @RegisterExtension
    val text = SnapshotExtension(format = TextSnapshotFormat)

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
