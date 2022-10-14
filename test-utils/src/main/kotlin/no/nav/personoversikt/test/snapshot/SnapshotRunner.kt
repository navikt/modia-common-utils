package no.nav.personoversikt.test.snapshot

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.NoSuchFileException

interface SnapshotRunner {
    fun assertMatches(value: Any?)
    fun updateSnapshot(value: Any?)

    interface Fileformat {
        val fileExtension: String
        fun write(value: Any): String
        fun read(value: String): Any
    }
}

class SnapshotRunnerImpl(
    path: String = "src/test/resources/snapshots",
    private val format: SnapshotRunner.Fileformat,
    private val debug: Boolean = false
) : SnapshotRunner {
    private val path: String
    private var name: String? = null
    private var counter: Int = 0
    private var hadMissingFile: Boolean = false
    init {
        if (path.endsWith("/")) {
            this.path = path
        } else {
            this.path = "$path/"
        }
        File(this.path).mkdirs()
    }

    fun beforeSnapshotRunner(name: String) {
        counter = 0
        this.name = name
    }

    fun afterSnapshotRunner() {
        if (hadMissingFile) {
            throw IllegalStateException("Snapshot did not exist, but was created. Rerun to verify.")
        }
    }

    override fun assertMatches(value: Any?) {
        assertMatches(getFile(counter++), value)
    }

    override fun updateSnapshot(value: Any?) {
        val file = getFile(counter++)
        if (read(file) == createSnapshot(value)) {
            throw IllegalStateException("Cannot update snapshot since they already are equal")
        } else {
            save(file, value)
            throw IllegalStateException("Snapshot updated, replace call with call to `assertMatches`")
        }
    }

    private fun assertMatches(file: File, value: Any?) {
        try {
            val snapshot = createSnapshot(value)
            if (debug) {
                Assertions.fail("Debugmode enabled.\nSnapshot:\n$snapshot")
            } else {
                assertEquals(read(file), snapshot)
            }
        } catch (e: NoSuchFileException) {
            save(file, value)
            assertMatches(file, value)
            hadMissingFile = true
        }
    }

    private fun getFile(id: Int): File {
        return this.name
            ?.let { File("$path$it-$id.${format.fileExtension}") }
            ?: throw IllegalStateException("No name...")
    }

    private fun save(file: File, value: Any?) {
        Files.writeString(file.toPath(), createSnapshot(value), StandardCharsets.UTF_8)
    }

    private fun read(file: File): String {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8)
    }

    private fun createSnapshot(value: Any?): String {
        if (value == null) {
            return "null"
        } else if (value is String) {
            try {
                return format.write(format.read(value))
            } catch (e: Exception) {
                // Not parseable to something useful
            }
        }
        return format.write(value)
    }
}
