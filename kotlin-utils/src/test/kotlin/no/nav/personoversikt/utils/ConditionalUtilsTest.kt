package no.nav.personoversikt.utils

import no.nav.personoversikt.utils.ConditionalUtils.ifNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ConditionalUtilsTest {
    @Test
    internal fun `should run if all values are non-null`() {
        var runCount = 0
        ifNotNull("A") { runCount++ }
        ifNotNull("A", "B") { _, _ -> runCount++ }
        ifNotNull("A", "B", "C") { _, _, _ -> runCount++ }
        ifNotNull("A", "B", "C", "D") { _, _, _, _ -> runCount++ }
        ifNotNull("A", "B", "C", "D", "E") { _, _, _, _, _ -> runCount++ }
        ifNotNull("A", "B", "C", "D", "E", "F") { _, _, _, _, _, _ -> runCount++ }

        assertTrue(runCount == 6)
    }

    @Test
    internal fun `should not run if a single value is null`() {
        var runCount = 0
        ifNotNull(null) { runCount++ }
        ifNotNull(null, "B") { _, _ -> runCount++ }
        ifNotNull(null, "B", "C") { _, _, _ -> runCount++ }
        ifNotNull(null, "B", "C", "D") { _, _, _, _ -> runCount++ }
        ifNotNull(null, "B", "C", "D", "E") { _, _, _, _, _ -> runCount++ }
        ifNotNull(null, "B", "C", "D", "E", "F") { _, _, _, _, _, _ -> runCount++ }
        ifNotNull("A", null) { _, _ -> runCount++ }
        ifNotNull("A", "B", null) { _, _, _ -> runCount++ }
        ifNotNull("A", "B", "C", null) { _, _, _, _ -> runCount++ }
        ifNotNull("A", "B", "C", "D", null) { _, _, _, _, _ -> runCount++ }
        ifNotNull("A", "B", "C", "D", "E", null) { _, _, _, _, _, _ -> runCount++ }

        assertTrue(runCount == 0)
    }
}
