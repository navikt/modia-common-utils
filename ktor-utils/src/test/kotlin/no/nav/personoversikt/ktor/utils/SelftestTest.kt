package no.nav.personoversikt.ktor.utils

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

internal class SelftestTest {
    @BeforeEach
    internal fun setup() {
        Selftest.restart()
    }

    @Test
    internal fun `should mount a route at root without configuration`() = testApplication {
        application {
            install(Selftest.Plugin) {
                appname = "testapp"
                version = "1.0.0"
            }
        }

        assertNaisRoutes(
            heading = "Should be ok if no reporters are registered",
            isAlive = HttpStatusCode.OK,
            isReady = HttpStatusCode.OK,
        )

        client
            .get("/internal/selftest")
            .assertEquals(
                status = HttpStatusCode.OK,
                content = """
                    Appname: testapp
                    Version: 1.0.0
                """.trimIndent(),
            )
    }

    @Test
    internal fun `should mount a route at correct contextpath`() = testApplication {
        application {
            install(Selftest.Plugin) {
                appname = "testapp"
                version = "1.0.0"
                contextpath = "myapp"
            }
        }

        assertNaisRoutes(
            heading = "Should be ok if no reporters are registered",
            contextpath = "myapp",
            isAlive = HttpStatusCode.OK,
            isReady = HttpStatusCode.OK,
        )

        client
            .get("myapp/internal/selftest")
            .assertEquals(
                status = HttpStatusCode.OK,
                content = """
                    Appname: testapp
                    Version: 1.0.0
                """.trimIndent(),
            )
    }

    @Test
    internal fun `should not be ready before every dependency has reported ok`() = testApplication {
        // Must alias plugin to ensure Selftest.kt is only loaded once by the classloader during test
        val plugin = Selftest.Plugin

        application {
            install(plugin) {
                appname = "testapp"
                version = "1.0.0"
            }
        }
        val reporter = Selftest.Reporter("dependency", critical = false)

        assertNaisRoutes(
            heading = "isReady should fail until dependency is marked as ok",
            isAlive = HttpStatusCode.OK,
            isReady = HttpStatusCode.InternalServerError,
        )

        reporter.reportOk()

        assertNaisRoutes(
            heading = "isReady should succeed when dependency is marked as ok",
            isAlive = HttpStatusCode.OK,
            isReady = HttpStatusCode.OK,
        )
    }

    @Test
    internal fun `should mark as not alive if critical errors occur`() = testApplication {
        // Must alias plugin to ensure Selftest.kt is only loaded once by the classloader during test
        val plugin = Selftest.Plugin
        application {
            install(plugin) {
                appname = "testapp"
                version = "1.0.0"
            }
        }
        val nonCritical = Selftest.Reporter("dependency", critical = false)
        val critical = Selftest.Reporter("other-dependency", critical = true)

        assertNaisRoutes(
            heading = "isReady should fail until dependencies are marked as ok",
            isAlive = HttpStatusCode.OK,
            isReady = HttpStatusCode.InternalServerError,
        )

        nonCritical.reportOk()
        critical.reportOk()

        assertNaisRoutes(
            heading = "isReady should succeed when dependencies are marked as ok",
            isAlive = HttpStatusCode.OK,
            isReady = HttpStatusCode.OK,
        )

        nonCritical.reportError(IllegalStateException("Something non-critical is wrong"))
        critical.reportOk()

        assertNaisRoutes(
            heading = "isReady should succeed when error is non-critical",
            isAlive = HttpStatusCode.OK,
            isReady = HttpStatusCode.OK,
        )

        nonCritical.reportOk()
        critical.reportError(IllegalStateException("Something critical is wrong"))

        assertNaisRoutes(
            heading = "isReady/isAlive should both fail when error is critical",
            isAlive = HttpStatusCode.InternalServerError,
            isReady = HttpStatusCode.InternalServerError,
        )
    }

    @Test
    internal fun `should list all dependencies in selftest`() = testApplication {
        val plugin = Selftest.Plugin
        application {
            install(plugin) {
                appname = "testapp"
                version = "1.0.0"
            }
        }
        val nonCritical = Selftest.Reporter("dependency", critical = false)
        val critical = Selftest.Reporter("other-dependency", critical = true)

        client
            .get("/internal/selftest")
            .assertEquals(
                status = HttpStatusCode.OK,
                content = """
                    Appname: testapp
                    Version: 1.0.0
                    
                    Name: dependency  Status: Registered
                    Name: other-dependency (Critical) Status: Registered
                """.trimIndent(),
            )

        nonCritical.reportOk()
        critical.reportOk()

        client
            .get("/internal/selftest")
            .assertEquals(
                status = HttpStatusCode.OK,
                content = """
                    Appname: testapp
                    Version: 1.0.0
                    
                    Name: dependency  Status: OK
                    Name: other-dependency (Critical) Status: OK
                """.trimIndent(),
            )

        nonCritical.reportError(IllegalStateException("Non critical error"))
        critical.reportError(IllegalStateException("Critical error"))

        client
            .get("/internal/selftest")
            .assertEquals(
                status = HttpStatusCode.OK,
                content = """
                    Appname: testapp
                    Version: 1.0.0
                    
                    Name: dependency  Status: KO: Non critical error
                    Name: other-dependency (Critical) Status: KO: Critical error
                """.trimIndent(),
            )
    }

    private suspend fun ApplicationTestBuilder.assertNaisRoutes(
        heading: String? = null,
        contextpath: String? = null,
        isAlive: HttpStatusCode,
        isReady: HttpStatusCode
    ) {
        val base = if (contextpath != null) "$contextpath/" else ""
        assertAll(
            heading = heading,
            {
                runBlocking {
                    client
                        .get("${base}internal/isAlive")
                        .assertEquals(
                            status = isAlive,
                            content = if (isAlive == HttpStatusCode.OK) "Alive" else "Not alive",
                        )
                }
            },
            {
                runBlocking {
                    client
                        .get("${base}internal/isReady")
                        .assertEquals(
                            status = isReady,
                            content = if (isReady == HttpStatusCode.OK) "Ready" else "Not ready",
                        )
                }
            }
        )
    }

    private suspend fun HttpResponse.assertEquals(status: HttpStatusCode, content: String) {
        assertEquals(status, this.status)
        assertEquals(content, bodyAsText().trim())
    }
}
