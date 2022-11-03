package no.nav.personoversikt.common.utils

import no.nav.personoversikt.common.utils.StringUtils.addPrefixIfMissing
import no.nav.personoversikt.common.utils.StringUtils.cutoff
import no.nav.personoversikt.common.utils.StringUtils.indicesOf
import no.nav.personoversikt.common.utils.StringUtils.isLetters
import no.nav.personoversikt.common.utils.StringUtils.isNumeric
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class StringUtilsTest {

    @Test
    internal fun `isNumeric should be true when text only contains digits`() {
        assertTrue("1234".isNumeric())
    }

    @Test
    internal fun `isNumeric should be false when text contains anything but digits`() {
        assertFalse("123#4".isNumeric())
        assertFalse("a1234".isNumeric())
        assertFalse("abc".isNumeric())
    }

    @Test
    internal fun `isLetters should be true when text only contains letters`() {
        assertTrue("abcd".isLetters())
    }

    @Test
    internal fun `isLetters should be false when text contains anything but digits`() {
        assertFalse("abcd#".isLetters())
        assertFalse("abcd.".isLetters())
        assertFalse("abc1".isLetters())
    }

    @Test
    internal fun `cutoff should not shorten text if size is below threshold`() {
        val text = "abcdef"
        assertEquals(text, text.cutoff(20))
    }

    @Test
    internal fun `cutoff should shorten text if size is above threshold`() {
        val text = "abcdef"
        assertEquals("ab...", text.cutoff(5))
    }

    @Test
    internal fun `indicesOf should return all indices of matches`() {
        val text = "abc.def.ghi"
        val indices = text.indicesOf(".")
        assertEquals(listOf(3, 7), indices)
    }

    @Test
    internal fun `'addPrefixIfMissing' should not add prefix if already present`() {
        val text = "bearer 123"
        assertEquals(text, text.addPrefixIfMissing("bearer "))
        assertEquals(text, text.addPrefixIfMissing("Bearer ", ignoreCase = true))
    }

    @Test
    internal fun `'addPrefixIfMissing' should add prefix if missing`() {
        val text = "prefix 123"
        assertEquals(text, text.addPrefixIfMissing("prefix "))
        assertEquals("Prefix $text", text.addPrefixIfMissing("Prefix ", ignoreCase = false))
    }
}
