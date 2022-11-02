package no.nav.personoversikt.common.logging

import net.logstash.logback.marker.Markers
import no.nav.personoversikt.common.logging.Logging.LOGTYPE_KEY
import no.nav.personoversikt.common.logging.TjenestekallLogger.Level
import org.slf4j.Marker

private typealias Fields = Map<String, Any?>
private typealias Tags = Map<String, Any?>

interface TjenestekallLogger {
    enum class Level {
        INFO,
        WARN,
        ERROR,
    }

    fun raw(level: Level, message: String, markers: Marker?, exception: Throwable?)
    fun info(header: String, fields: Fields, tags: Tags = emptyMap())
    fun warn(header: String, fields: Fields, tags: Tags = emptyMap())
    fun error(header: String, fields: Fields, tags: Tags = emptyMap(), exception: Throwable)
}
object TjenestekallLogg : TjenestekallLogger {
    val raw = Logging.secureLog
    private val logtypemap = mutableMapOf<LogType, TjenestekallLogger>()
    private val separator = "-".repeat(84)

    @JvmInline
    value class LogType(val value: String)

    fun withLogType(type: LogType): TjenestekallLogger {
        return logtypemap.getOrPut(type) {
            object : TjenestekallLogger {
                override fun info(header: String, fields: Fields, tags: Tags) {
                    TjenestekallLogg.info(header, fields, tags + (LOGTYPE_KEY to type.value))
                }

                override fun warn(header: String, fields: Fields, tags: Tags) {
                    TjenestekallLogg.warn(header, fields, tags + (LOGTYPE_KEY to type.value))
                }

                override fun error(header: String, fields: Fields, tags: Tags, exception: Throwable) {
                    TjenestekallLogg.error(header, fields, tags + (LOGTYPE_KEY to type.value), exception)
                }

                override fun raw(level: Level, message: String, markers: Marker?, exception: Throwable?) {
                    TjenestekallLogg.raw(level, message, markers, exception)
                }
            }
        }
    }

    override fun info(
        header: String,
        fields: Fields,
        tags: Tags,
    ) = log(Level.INFO, header, fields, tags)

    override fun warn(
        header: String,
        fields: Fields,
        tags: Tags,
    ) = log(Level.WARN, header, fields, tags)

    override fun error(
        header: String,
        fields: Fields,
        tags: Tags,
        exception: Throwable
    ) = log(Level.ERROR, header, fields, tags, exception)

    override fun raw(level: Level, message: String, markers: Marker?, exception: Throwable?) {
        val loggerFn: (Marker?, String, Throwable?) -> Unit = when (level) {
            Level.INFO -> raw::info
            Level.WARN -> raw::warn
            Level.ERROR -> raw::error
        }
        loggerFn(markers, message, exception)
    }

    fun format(header: String, fields: Map<String, Any?>): String = buildString {
        appendLine(header)
        appendSeparator()
        for ((key, value) in fields) {
            appendLine("$key: $value")
        }
        appendSeparator()
    }

    fun format(header: String, body: String): String = buildString {
        appendLine(header)
        appendSeparator()
        appendLine(body)
        appendSeparator()
    }

    private fun log(
        level: Level,
        header: String,
        fields: Fields,
        tags: Tags,
        exception: Throwable? = null,
    ) = raw(level, format(header, fields), Markers.appendEntries(tags), exception)

    private fun StringBuilder.appendSeparator() = appendLine(separator)
}
