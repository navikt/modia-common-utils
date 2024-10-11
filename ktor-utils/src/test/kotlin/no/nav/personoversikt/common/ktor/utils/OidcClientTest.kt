package no.nav.personoversikt.common.ktor.utils

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class OidcClientTest {
    @Test
    internal fun `should be able to deserialize well-known json`() =
        runBlocking {
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content =
                            ByteReadChannel(
                                """{
                    |"issuer":"wk-issuer", 
                    |"jwks_uri": "http://jwks.uri", 
                    |"response_types_supported": ["code", "id_token"],
                    |"authorization_endpoint": "http://auth.io",
                    |"token_endpoint": "http://token.io"
                    |}
                                """.trimMargin(),
                            ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = OidcClient("http://dummy.test", mockEngine)
            val config = client.fetch()
            Assertions.assertEquals("wk-issuer", config.issuer)
            Assertions.assertEquals("http://jwks.uri", config.jwksUrl)
        }
}
