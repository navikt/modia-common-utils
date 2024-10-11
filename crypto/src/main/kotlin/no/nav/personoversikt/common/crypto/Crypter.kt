package no.nav.personoversikt.common.crypto

class Crypter(
    secret: String,
) {
    private val password = secret.substring(secret.length / 2)
    private val salt = secret.removePrefix(password)
    private val key = AES.generateKey(password, salt)

    fun encryptOrThrow(plaintext: String): String = encrypt(plaintext).getOrThrow()

    fun decryptOrThrow(ciphertext: String): String = decrypt(ciphertext).getOrThrow()

    fun encrypt(plaintext: String): Result<String> =
        runCatching { plaintext.toByteArray() }
            .mapCatching { AES.encrypt(it, key) }
            .mapCatching { Encoding.encode(it) }

    fun decrypt(ciphertext: String): Result<String> =
        runCatching { Encoding.decode(ciphertext) }
            .mapCatching { AES.decrypt(it, key) }
            .mapCatching { String(it) }
}
