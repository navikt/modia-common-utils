package no.nav.personoversikt.utils

import no.nav.personoversikt.utils.StringUtils.cutoff
import no.nav.personoversikt.utils.StringUtils.indicesOf
import no.nav.personoversikt.utils.StringUtils.isLetters
import no.nav.personoversikt.utils.StringUtils.isNumeric
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
}
