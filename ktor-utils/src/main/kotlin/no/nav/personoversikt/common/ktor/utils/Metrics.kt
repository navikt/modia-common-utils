package no.nav.personoversikt.common.ktor.utils

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

object Metrics {
    class Config {
        var contextpath: String = ""
        var registry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

        // Expose configuration of MicrometerMetrics, defaults copied
        var metricName: String = "ktor.http.server.requests"
        var meterBinders: List<MeterBinder> = listOf(
            ClassLoaderMetrics(),
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
            JvmThreadMetrics(),
            FileDescriptorMetrics()
        )
        var distributionStatisticConfig: DistributionStatisticConfig =
            DistributionStatisticConfig.Builder().percentiles(0.5, 0.9, 0.95, 0.99).build()

        internal var timerBuilder: Timer.Builder.(ApplicationCall, Throwable?) -> Unit = { _, _ -> }
        fun timers(block: Timer.Builder.(ApplicationCall, Throwable?) -> Unit) {
            timerBuilder = block
        }
    }

    val Plugin = createApplicationPlugin("Metrics", Metrics::Config) {
        val config = pluginConfig
        with(application) {
            install(MicrometerMetrics) {
                registry = config.registry
                metricName = config.metricName
                meterBinders = config.meterBinders
                distributionStatisticConfig = config.distributionStatisticConfig
                timers(block = config.timerBuilder)
            }

            routing {
                route(config.contextpath) {
                    route("internal") {
                        get("metrics") {
                            call.respondText(config.registry.scrape())
                        }
                    }
                }
            }
        }
    }
}
