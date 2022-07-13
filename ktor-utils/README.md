# Ktor Utils

Inneholder små funksjoner, moduler og plugins for forenkling av oppsett av ktor apper


## Metrics

**Oppsett:**
```kotlin
application {
    install(Metrics.Plugin) {
        contextpath = "path-to-app-if-any (optional)"
        // Configuration of MicrometerMetrics exposed as well
        meterBinders = emptyList() // e.g disable all standard metrics
    }
}
```

**Lage en egne metrikker:**
```kotlin
val counter = Metrics.Registry.counter("my-counter")
val timer = Metrics.Registry.timer("my-timer")
val gauge = Metrics.Registry.gauge("my-gauge")
```

# Security

**Oppsett:**
```kotlin
application {
    val security = Security(
        AuthProviderConfig(
            name = "FirstIDP", 
            jwks = "https://url.to.jwks/"
        ),
        AuthProviderConfig(
            name = "SecondIDP",
            jwks = "https://other.to.jwks/",
            cookies = listOf(
                AuthCookie(name = "my-auth"),
                AuthCookie(name = "enc-auth", encryptionKey = "my-super-secret-key")
            )
        )
    )
    
    install(Authentication) {
        if (useMock) {
            security.setupMock(SubjectPrincipal("Z999999"))
        } else {
            security.setupJWT()
        }
    }
}
```

**Beskytt ett endepunkt:**
```kotlin
routing {
    authenticate(*security.authproviders) {
        // your route config
    }
}
```

**NB!** Om man bare har en `AuthProviderConfig` kan man sette `name = null` og deretter ikek sende med `*security.authproviders` til `authenticate`.

**NB!** Om man har flere `AuthProviderConfig` vil plugin'en teste hver enkelt inntil den finner en gyldig innlogging. Den vil derfor ikke nødvendigvis sikre at bruker er logget inn flere steder.

**NB!** When configuring multiple `AuthProviderConfig` the plugin will test each configuration until a valid `SubjectPrincipal` is found. As such this does not enforce that a user is logged into every IDP configured.
