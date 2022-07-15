package no.nav.personoversikt.ktor.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@OptIn(DelicateCoroutinesApi::class)
object Selftest {
    class Config(
        var appname: String = "Not set",
        var version: String = "Not set",
        var contextpath: String = "",
    )

    sealed class Event(val reporter: Reporter)
    class InitEvent(reporter: Reporter) : Event(reporter)
    class OkEvent(reporter: Reporter) : Event(reporter)
    class ErrorEvent(reporter: Reporter, val error: Throwable) : Event(reporter)

    private val statusmap = mutableMapOf<String, Event>()
    private val channel = Channel<Event>(capacity = Channel.BUFFERED)
    private var aggregator = GlobalScope.launch {
        channel.receiveAsFlow().collect {
            statusmap[it.reporter.name] = it
        }
    }

    internal fun restart() {
        statusmap.clear()
        aggregator = GlobalScope.launch {
            channel.receiveAsFlow().collect {
                statusmap[it.reporter.name] = it
            }
        }
    }

    val Status: Map<String, Event> = statusmap

    class Reporter(val name: String, val critical: Boolean) {
        init {
            runBlocking {
                channel.send(InitEvent(this@Reporter))
            }
        }

        suspend fun reportOk() {
            channel.send(OkEvent(this))
        }

        suspend fun reportError(error: Throwable) {
            channel.send(ErrorEvent(this, error))
        }

        suspend fun ping(fn: suspend () -> Unit) {
            try {
                fn()
                reportOk()
            } catch (e: Throwable) {
                reportError(e)
            }
        }
    }

    val Plugin = createApplicationPlugin("Selftest", { Config() }) {
        val plugin = this
        val config = pluginConfig
        with(application) {
            plugin.on(MonitoringEvent(ApplicationStopPreparing)) {
                aggregator.cancel()
            }

            routing {
                route(config.contextpath) {
                    route("internal") {
                        get("isAlive") {
                            val hasCriticalError = Status.values.any { it is ErrorEvent && it.reporter.critical }

                            if (hasCriticalError) {
                                call.respondText("Not alive", status = HttpStatusCode.InternalServerError)
                            } else {
                                call.respondText("Alive")
                            }
                        }

                        get("isReady") {
                            val isReady = Status.values.all { it !is InitEvent }
                            val noCriticalError = Status.values.none { it is ErrorEvent && it.reporter.critical }

                            if (isReady && noCriticalError) {
                                call.respondText("Ready")
                            } else {
                                call.respondText("Not ready", status = HttpStatusCode.InternalServerError)
                            }
                        }

                        get("selftest") {
                            call.respondText {
                                buildString {
                                    appendLine("Appname: ${config.appname}")
                                    appendLine("Version: ${config.version}")
                                    appendLine()
                                    for (result in Status.values) {
                                        val critical = if (result.reporter.critical) "(Critical)" else ""
                                        val status = when (result) {
                                            is InitEvent -> "Registered"
                                            is OkEvent -> "OK"
                                            is ErrorEvent -> "KO: ${result.error.message}"
                                        }
                                        appendLine("Name: ${result.reporter.name} $critical Status: $status")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
