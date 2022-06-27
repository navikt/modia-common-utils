package no.nav.personoversikt.crypto

import no.nav.personoversikt.crypto.Encoding.isBase64Encoded
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.ThrowingSupplier
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class CrypterTest {

    private val crypter = Crypter("dummy")

    companion object {
        @JvmStatic
        fun plaintext(): List<Arguments> = listOf(
            Arguments.of("💩"),
            Arguments.of("plaintext"),
            Arguments.of("https://github.com/navikt/modiapersonoversikt-api/blob/dev/web/src/test/java/no/nav/modiapersonoversikt/infrastructure/kabac/utils/CombiningAlgorithmTest.kt"),
        )
    }

    @ParameterizedTest
    @MethodSource("plaintext")
    internal fun `should encrypt and decrypt tokene`(plaintext: String) {
        val ciphertext = crypter.encryptOrThrow(plaintext)
        assertEquals(plaintext, crypter.decryptOrThrow(ciphertext))
        assertTrue(ciphertext.isBase64Encoded())
    }

    @Test
    internal fun `should throw exception on decryptOrThrow`() {
        assertThrows<IllegalArgumentException> { crypter.decryptOrThrow("") }
    }

    @Test
    internal fun `should not throw exception on decrypt`() {
        val result = assertDoesNotThrow(
            ThrowingSupplier {
                crypter.decrypt("")
            }
        )
        assertTrue(result.isFailure)
    }
}
