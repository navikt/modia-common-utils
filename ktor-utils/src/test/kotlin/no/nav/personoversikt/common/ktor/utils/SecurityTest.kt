package no.nav.personoversikt.common.ktor.utils

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.util.pipeline.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SecurityTest {
    @Test
    internal fun `should use auth-header if present`() {
        testIntercept({
            headers {
                append(HttpHeaders.Authorization, "Bearer headertoken")
                append(HttpHeaders.Cookie, "test=cookietoken;other=othertoken")
            }
        }
        ) {
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
    }

    @Test
    internal fun `should use first non-null cookie value`() {
        testIntercept({
            headers {
                append(HttpHeaders.Authorization, "Bearer headertoken")
                append(HttpHeaders.Cookie, "test=cookietoken;other=othertoken")
            }}
        ){

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
    }

    @Test
    internal fun `should use be able to get tokens for multiple providers`() {
        testIntercept({
            headers {
                append(HttpHeaders.Authorization, "Bearer headertoken")
                append(HttpHeaders.Cookie, "test=cookietoken;other=othertoken")
            }

        }) {

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
    }

    private fun testIntercept(block: HttpRequestBuilder.() -> Unit, interceptBlock: suspend PipelineContext<*, PipelineCall>.() -> Unit) = testApplication {
        application {
            intercept(ApplicationCallPipeline.Call) {
                interceptBlock()
            }
        }

        client.get(block)
    }
}
