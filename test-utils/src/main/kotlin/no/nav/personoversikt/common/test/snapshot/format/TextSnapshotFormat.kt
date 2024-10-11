package no.nav.personoversikt.common.test.snapshot.format

import no.nav.personoversikt.common.test.snapshot.SnapshotRunner

object TextSnapshotFormat : SnapshotRunner.Fileformat {
    override val fileExtension: String = "txt"

    override fun write(value: Any): String = value.toString()

    override fun read(value: String): Any = value
}
