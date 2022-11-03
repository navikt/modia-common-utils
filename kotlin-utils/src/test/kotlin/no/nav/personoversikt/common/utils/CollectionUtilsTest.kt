package no.nav.personoversikt.common.utils

import no.nav.personoversikt.common.utils.CollectionUtils.isNotNullOrEmpty
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CollectionUtilsTest {
    @Test
    internal fun `should return true for non-empty collection`() {
        assertTrue(listOf("1").isNotNullOrEmpty())
        assertTrue(setOf("1").isNotNullOrEmpty())
    }

    @Test
    internal fun `should return false for empty collection`() {
        assertFalse(emptyList<String>().isNotNullOrEmpty())
        assertFalse(emptySet<String>().isNotNullOrEmpty())
    }

    @Test
    internal fun `should return false for null`() {
        assertFalse((null as List<String>?).isNotNullOrEmpty())
        assertFalse((null as Set<String>?).isNotNullOrEmpty())
    }
}
