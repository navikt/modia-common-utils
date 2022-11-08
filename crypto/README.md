# Crypto

Felleskomponent for krypering/dekryptering vha AES/GCM.


## Ta ibruk:
```xml
<dependency>
    <groupId>com.github.navikt.modia-common-utils</groupId>
    <artifactId>crypto</artifactId>
    <version>???</version>
</dependency>
```

### Standard bruk

`Crypter` gjør automatisk encoding vha base64 (url-safe), slik at verdiene man får ut kan brukes i de fleste tilfeller.
Om man trenger mer kontroll på input/output kan man bruke `AES` funksjonene direkte (se nedenfor).

```kotlin
val secret = "secret"
val crypter = Crypter(secret)

val plaintext: String = "noe sensitiv informasjon"
val ciphertextResult: Result<String> = crypter.encrypt(plaintext)

println(ciphertextResult.getOrThrow() == plaintext) 
```


### Utvidet bruk
Trenger man mer kontroll, eller jobber med datatyper utenom `String` kan man bruke `AES` direkte.

```kotlin
val password = "password"
val salt = "salt"
val key = AES.generateKey(password, salt)

val plaintext: ByteArray = "noe sensitiv informasjon".toByteArray()
val ciphertext: ByteArray = AES.encrypt(plaintext, key)

println(AES.decrypt(ciphertext, key) == plaintext)
```