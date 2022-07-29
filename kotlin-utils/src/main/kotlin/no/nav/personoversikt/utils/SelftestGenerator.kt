package no.nav.personoversikt.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@OptIn(DelicateCoroutinesApi::class)
class SelftestGenerator(private val config: Config) {
    companion object {
        private val statusmap = mutableMapOf<String, Event>()
        private val channel = Channel<Event>(capacity = Channel.BUFFERED)
        private var aggregator = GlobalScope.launch {
            channel.receiveAsFlow().collect {
                statusmap[it.reporter.name] = it
            }
        }

        internal fun register(event: InitEvent) {
            statusmap[event.reporter.name] = event
        }

        internal fun restart() {
            statusmap.clear()
            aggregator = GlobalScope.launch {
                channel.receiveAsFlow().collect {
                    statusmap[it.reporter.name] = it
                }
            }
        }

        fun stop() {
            aggregator.cancel()
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
            register(InitEvent(this))
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
}
