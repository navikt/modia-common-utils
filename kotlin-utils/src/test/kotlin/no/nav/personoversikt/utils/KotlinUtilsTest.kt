package no.nav.personoversikt.utils

import no.nav.personoversikt.utils.KotlinUtils.and
import no.nav.personoversikt.utils.KotlinUtils.inRange
import no.nav.personoversikt.utils.KotlinUtils.inRangeInclusive
import no.nav.personoversikt.utils.KotlinUtils.or
import no.nav.personoversikt.utils.KotlinUtils.xor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class KotlinUtilsTest {
    @Test
    internal fun `inRange should return true if in range`() {
        assertTrue(200 inRange (200 to 300))
        assertTrue(299 inRange (200 to 300))
    }

    @Test
    internal fun `inRange should not be inclusive`() {
        assertFalse(300 inRange (200 to 300))
    }

    @Test
    internal fun `inRangeInclusive shouldbe inclusive`() {
        assertTrue(300 inRangeInclusive (200 to 300))
    }

    val fnTrue: (Unit) -> Boolean = { true }
    val fnFalse: (Unit) -> Boolean = { false }

    @Test
    internal fun `'or' should or results from predicates`() {
        assertFalse((fnFalse or fnFalse).invoke(Unit))
        assertTrue((fnFalse or fnTrue).invoke(Unit))
        assertTrue((fnTrue or fnFalse).invoke(Unit))
        assertTrue((fnTrue or fnTrue).invoke(Unit))
    }

    @Test
    internal fun `'and' should and results from predicates`() {
        assertFalse((fnFalse and fnFalse).invoke(Unit))
        assertFalse((fnFalse and fnTrue).invoke(Unit))
        assertFalse((fnTrue and fnFalse).invoke(Unit))
        assertTrue((fnTrue and fnTrue).invoke(Unit))
    }

    @Test
    internal fun `'xor' should xor results from predicates`() {
        assertFalse((fnFalse xor fnFalse).invoke(Unit))
        assertTrue((fnFalse xor fnTrue).invoke(Unit))
        assertTrue((fnTrue xor fnFalse).invoke(Unit))
        assertFalse((fnTrue xor fnTrue).invoke(Unit))
    }
}
