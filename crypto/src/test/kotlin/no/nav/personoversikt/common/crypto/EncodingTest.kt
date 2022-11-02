package no.nav.personoversikt.common.crypto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Base64

internal class EncodingTest {

    @Test
    internal fun `should use urlBase64Encoder`() {
        val plaintext = "dummy"
        val encoded = Encoding.encode(plaintext.toByteArray())
        val decoded = Base64.getUrlDecoder().decode(encoded)

        assertEquals(plaintext, String(decoded))
    }
}
