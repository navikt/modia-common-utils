package no.nav.personoversikt.common.ktor.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
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

    private fun createCall(block: TestApplicationRequest.() -> Unit): ApplicationCall {
        val engine = TestApplicationEngine()
        return engine.createCall(setup = block)
    }
}
