package no.nav.personoversikt.common.utils

import kotlinx.coroutines.*

class SelftestGenerator private constructor(private var config: Config) {
    companion object {
        private var instance = SelftestGenerator(Config())
        fun getInstance(config: Config): SelftestGenerator {
            instance.config = config
            return instance
        }
    }
    open class Config(
        var appname: String = "Not set",
        var version: String = "Not set",
    )

    sealed class Event(val reporter: Reporter)
    class InitEvent(reporter: Reporter) : Event(reporter)
    class OkEvent(reporter: Reporter) : Event(reporter)
    class ErrorEvent(reporter: Reporter, val error: Throwable) : Event(reporter)

    private val statusmap = mutableMapOf<String, Event>()

    internal fun register(event: Event) {
        statusmap[event.reporter.name] = event
    }

    internal fun clear() {
        statusmap.clear()
    }

    fun isAlive(): Boolean {
        return statusmap.values.none { it is ErrorEvent && it.reporter.critical }
    }

    fun isReady(): Boolean {
        val isReady = statusmap.values.all { it !is InitEvent }
        val noCriticalError = statusmap.values.none { it is ErrorEvent && it.reporter.critical }
        return isReady && noCriticalError
    }

    fun scrape() = buildString {
        appendLine("Appname: ${config.appname}")
        appendLine("Version: ${config.version}")
        appendLine()
        for (result in statusmap.values) {
            val critical = if (result.reporter.critical) "(Critical)" else ""
            val status = when (result) {
                is InitEvent -> "Registered"
                is OkEvent -> "OK"
                is ErrorEvent -> "KO: ${result.error.message}"
            }
            appendLine("Name: ${result.reporter.name} $critical Status: $status")
        }
    }

    class Reporter(val name: String, val critical: Boolean) {
        init {
            instance.register(InitEvent(this))
        }

        fun reportOk() {
            instance.register(OkEvent(this))
        }

        fun reportError(error: Throwable) {
            instance.register(ErrorEvent(this, error))
        }

        fun ping(fn: suspend () -> Unit) {
            try {
                runBlocking(Dispatchers.IO) {
                    fn()
                }
                reportOk()
            } catch (e: Throwable) {
                reportError(e)
            }
        }
    }
}
