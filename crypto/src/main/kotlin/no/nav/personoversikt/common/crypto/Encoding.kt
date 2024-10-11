package no.nav.personoversikt.common.crypto

import java.util.Base64

object Encoding {
    private val encoder = Base64.getUrlEncoder()
    private val decoder = Base64.getUrlDecoder()

    fun encode(bytes: ByteArray): String = encoder.encodeToString(bytes)

    fun decode(string: String): ByteArray = decoder.decode(string)

    fun String.isBase64Encoded(): Boolean =
        try {
            decoder.decode(this)
            true
        } catch (ex: Throwable) {
            false
        }
}
