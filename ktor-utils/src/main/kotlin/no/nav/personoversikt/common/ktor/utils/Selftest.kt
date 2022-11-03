package no.nav.personoversikt.common.ktor.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.personoversikt.common.utils.SelftestGenerator

object Selftest {
    class Config(
        appname: String = "Not set",
        version: String = "Not set",
        var contextpath: String = ""
    ) : SelftestGenerator.Config(appname, version)

    val Plugin = createApplicationPlugin("Selftest", { Config() }) {
        val config = pluginConfig
        val selftest = SelftestGenerator.getInstance(pluginConfig)
        with(application) {
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
