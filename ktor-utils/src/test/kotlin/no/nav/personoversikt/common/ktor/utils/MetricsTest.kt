package no.nav.personoversikt.common.ktor.utils

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class MetricsTest {
    @Test
    internal fun `should mount a route at root without configuration`() =
        testApplication {
            application {
                install(Metrics.Plugin)
            }
            val response = client.get("/internal/metrics")
            assertEquals(HttpStatusCode.OK, response.status)
            assertNotNull(response.bodyAsText())
        }

    @Test
    internal fun `should mount a route at correct contextpath`() =
        testApplication {
            application {
                install(Metrics.Plugin) {
                    contextpath = "myapp"
                }
            }
            val response = client.get("myapp/internal/metrics")
            assertEquals(HttpStatusCode.OK, response.status)
            assertNotNull(response.bodyAsText())
        }

    @Test
    internal fun `should expose metrics registered to the registry`() =
        testApplication {
            // Must alias plugin to ensure Metrics.kt is only loaded once by the classloader during test
            val plugin = Metrics.Plugin

            val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            application {
                install(plugin) {
                    registry = meterRegistry
                }
            }

            val counter = meterRegistry.counter("testcounter")
            counter.increment(13.0)

            val response = client.get("/internal/metrics")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("testcounter_total 13.0"))
        }
}
