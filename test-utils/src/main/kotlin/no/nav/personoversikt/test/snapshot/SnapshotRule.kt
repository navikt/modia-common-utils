package no.nav.personoversikt.test.snapshot

import no.nav.personoversikt.test.snapshot.format.JsonSnapshotFormat
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class SnapshotRule(
    path: String = "src/test/resources/snapshots",
    format: SnapshotRunner.Fileformat = JsonSnapshotFormat,
    debug: Boolean = false,
    private val runner: SnapshotRunnerImpl = SnapshotRunnerImpl(path, format, debug)
) : TestWatcher(), SnapshotRunner by runner {
    override fun starting(description: Description) {
        runner.beforeSnapshotRunner("${description.className}_${description.methodName}")
    }

    override fun finished(description: Description) {
        runner.afterSnapshotRunner()
    }
}
