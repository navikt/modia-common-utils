package no.nav.personoversikt.ktor.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.personoversikt.utils.SelftestGenerator

object Selftest {
    class Config(
        appname: String = "Not set",
        version: String = "Not set",
        var contextpath: String = ""
    ) : SelftestGenerator.Config(appname, version)

    val Plugin = createApplicationPlugin("Selftest", { Config() }) {
        val plugin = this
        val config = pluginConfig
        val selftest = SelftestGenerator(pluginConfig)
        with(application) {
            plugin.on(MonitoringEvent(ApplicationStopPreparing)) {
                SelftestGenerator.stop()
            }

            routing {
                route(config.contextpath) {
                    route("internal") {
                        get("isAlive") {
                            if (selftest.isAlive()) {
                                call.respondText("Alive")
                            } else {
                                call.respondText("Not alive", status = HttpStatusCode.InternalServerError)
                            }
                        }

                        get("isReady") {
                            if (selftest.isReady()) {
                                call.respondText("Ready")
                            } else {
                                call.respondText("Not ready", status = HttpStatusCode.InternalServerError)
                            }
                        }

                        get("selftest") {
                            call.respondText(selftest.scrape())
                        }
                    }
                }
            }
        }
    }
}
