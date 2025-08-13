package no.nav.personoversikt.common.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory

object Logging {
    const val LOGTYPE_KEY = "logtype"
    val TEAM_LOGS_MARKER: Marker = MarkerFactory.getMarker("TEAM_LOGS")
    private val faktiskSecureLog: Logger = LoggerFactory.getLogger("SecureLog")

    val auditLog: Logger = LoggerFactory.getLogger("AuditLogger")
    val teamLog: Logger = LoggerFactory.getLogger("TeamLog")
    val secureLog: Logger = DualLogger(faktiskSecureLog, teamLog, TEAM_LOGS_MARKER)

    // Logger til både secureLog og teamLog parallelt ved kall til secureLog.
    // Dette kan fjernes når secureLog skal fases ut.

    private class DualLogger(
        private val secureLog: Logger,
        private val teamLog: Logger,
        private val teamLogMarker: Marker,
    ) : Logger by secureLog {
        override fun info(
            marker: Marker?,
            msg: String?,
            t: Throwable?,
        ) = logToBoth { p, s ->
            p.info(marker, msg, t)
            s.info(teamLogMarker, msg, t)
        }

        override fun warn(
            marker: Marker?,
            msg: String?,
            t: Throwable?,
        ) = logToBoth { p, s ->
            p.warn(marker, msg, t)
            s.warn(teamLogMarker, msg, t)
        }

        override fun error(
            marker: Marker?,
            msg: String?,
            t: Throwable?,
        ) = logToBoth { p, s ->
            p.error(marker, msg, t)
            s.error(teamLogMarker, msg, t)
        }

        private inline fun logToBoth(action: (Logger, Logger) -> Unit) = action(secureLog, teamLog)
    }
}
