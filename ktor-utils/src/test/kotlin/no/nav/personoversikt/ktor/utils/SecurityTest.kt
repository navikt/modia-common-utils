package no.nav.personoversikt.ktor.utils

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SecurityTest {
    @Test
    internal fun `should use auth-header if present`() {
        val call = createCall {
            this.addHeader(HttpHeaders.Authorization, "Bearer headertoken")
            this.addHeader(HttpHeaders.Cookie, "test=cookietoken;other=othertoken")
        }
        val security = Security(
            Security.AuthProviderConfig(
                name = null,
                jwksConfig = Security.JwksConfig.JwksUrl("http://localhost.com", "issuer"),
                tokenLocations = listOf(
                    Security.TokenLocation.Cookie(name = "notfound"),
                    Security.TokenLocation.Header(),
                    Security.TokenLocation.Cookie(name = "test"),
                )
            )
        )
        val token = security.getToken(call)

        assertEquals(listOf("Bearer headertoken"), token)
    }

    @Test
    internal fun `should use first non-null cookie value`() {
        val call = createCall {
            this.addHeader(HttpHeaders.Authorization, "Bearer headertoken")
            this.addHeader(HttpHeaders.Cookie, "test=cookietoken;other=othertoken")
        }
        val security = Security(
            Security.AuthProviderConfig(
                name = null,
                jwksConfig = Security.JwksConfig.JwksUrl("http://localhost.com", "issuer"),
                tokenLocations = listOf(
                    Security.TokenLocation.Cookie(name = "notfound"),
                    Security.TokenLocation.Cookie(name = "test"),
                    Security.TokenLocation.Cookie(name = "other"),
                )
            )
        )

        val token = security.getToken(call)

        assertEquals(listOf("Bearer cookietoken"), token)
    }

    @Test
    internal fun `should use be able to get tokens for multiple providers`() {
        val call = createCall {
            this.addHeader(HttpHeaders.Authorization, "Bearer headertoken")
            this.addHeader(HttpHeaders.Cookie, "test=cookietoken;other=othertoken")
        }
        val baseprovider = Security.AuthProviderConfig(
            name = null,
            jwksConfig = Security.JwksConfig.JwksUrl("http://localhost.com", "issuer"),
            tokenLocations = emptyList()
        )
        val security = Security(
            baseprovider.copy(tokenLocations = listOf(Security.TokenLocation.Header())),
            baseprovider.copy(tokenLocations = listOf(Security.TokenLocation.Cookie(name = "test"))),
            baseprovider.copy(tokenLocations = listOf(Security.TokenLocation.Cookie(name = "other"))),
        )

        val token = security.getToken(call)

        assertEquals(listOf("Bearer headertoken", "Bearer cookietoken", "Bearer othertoken"), token)
    }

    @Test
    internal fun `should be able to deserialize well-known json`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""{"issuer":"wk-issuer", "jwks_uri": "http://jwks.uri", "response_types_supported": ["code", "id_token"]}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val wellKnownUrl = Security.JwksConfig.OidcWellKnownUrl("http://dummy.test", mockEngine)
        assertEquals("wk-issuer", wellKnownUrl.issuer)
        assertEquals("http://jwks.uri", wellKnownUrl.jwksUrl)
    }

    private fun createCall(block: TestApplicationRequest.() -> Unit): ApplicationCall {
        val engine = TestApplicationEngine()
        return engine.createCall(setup = block)
    }
}
