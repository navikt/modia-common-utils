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

    fun raw(
        level: Level,
        message: String,
        markers: Marker?,
        throwable: Throwable? = null,
    )

    fun info(
        header: String,
        fields: Fields,
        tags: Tags = emptyMap(),
        throwable: Throwable? = null,
    )

    fun warn(
        header: String,
        fields: Fields,
        tags: Tags = emptyMap(),
        throwable: Throwable? = null,
    )

    fun error(
        header: String,
        fields: Fields,
        tags: Tags = emptyMap(),
        throwable: Throwable? = null,
    )
}

object TjenestekallLogg : TjenestekallLogger {
    val raw = Logging.secureLog
    val teamRaw = Logging.teamLog
    private val logtypemap = mutableMapOf<String, TjenestekallLogger>()
    private val separator = "-".repeat(84)

    fun withLogType(type: String): TjenestekallLogger =
        logtypemap.getOrPut(type) {
            object : TjenestekallLogger {
                override fun info(
                    header: String,
                    fields: Fields,
                    tags: Tags,
                    throwable: Throwable?,
                ) {
                    TjenestekallLogg.info(header, fields, tags + (LOGTYPE_KEY to type), throwable)
                }

                override fun warn(
                    header: String,
                    fields: Fields,
                    tags: Tags,
                    throwable: Throwable?,
                ) {
                    TjenestekallLogg.warn(header, fields, tags + (LOGTYPE_KEY to type), throwable)
                }

                override fun error(
                    header: String,
                    fields: Fields,
                    tags: Tags,
                    throwable: Throwable?,
                ) {
                    TjenestekallLogg.error(header, fields, tags + (LOGTYPE_KEY to type), throwable)
                }

                override fun raw(
                    level: Level,
                    message: String,
                    markers: Marker?,
                    throwable: Throwable?,
                ) {
                    TjenestekallLogg.raw(level, message, markers, throwable)
                }
            }
        }

    override fun info(
        header: String,
        fields: Fields,
        tags: Tags,
        throwable: Throwable?,
    ) = log(Level.INFO, header, fields, tags, throwable)

    override fun warn(
        header: String,
        fields: Fields,
        tags: Tags,
        throwable: Throwable?,
    ) = log(Level.WARN, header, fields, tags, throwable)

    override fun error(
        header: String,
        fields: Fields,
        tags: Tags,
        throwable: Throwable?,
    ) = log(Level.ERROR, header, fields, tags, throwable)

    override fun raw(
        level: Level,
        message: String,
        markers: Marker?,
        throwable: Throwable?,
    ) {
        val loggerFn: (Marker?, String, Throwable?) -> Unit =
            when (level) {
                Level.INFO -> raw::info
                Level.WARN -> raw::warn
                Level.ERROR -> raw::error
            }
        loggerFn(markers, message, throwable)

        // Send logger til team logs parallell med secure logs i første omgang
        val teamMarkers =
            Markers.append(Logging.TEAM_LOGS_MARKER, "true").let { teamMarker ->
                markers?.let { teamMarker.and(it) }
                    ?: teamMarker
            }

        val teamLoggerFn: (Marker?, String, Throwable?) -> Unit =
            when (level) {
                Level.INFO -> teamRaw::info
                Level.WARN -> teamRaw::warn
                Level.ERROR -> teamRaw::error
            }
        teamLoggerFn(teamMarkers, message, throwable)
    }

    fun format(
        header: String,
        fields: Map<String, Any?>,
    ): String =
        buildString {
            appendLine(header)
            appendSeparator()
            for ((key, value) in fields) {
                appendLine("$key: $value")
            }
            appendSeparator()
        }

    fun format(
        header: String,
        body: String,
    ): String =
        buildString {
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
