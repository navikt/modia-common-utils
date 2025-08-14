package no.nav.personoversikt.common.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory

object Logging {
    const val LOGTYPE_KEY = "logtype"
    val TEAM_LOGS_MARKER: Marker = MarkerFactory.getMarker("TEAM_LOGS")
    val secureLog: Logger = LoggerFactory.getLogger("TeamLog")
    val auditLog: Logger = LoggerFactory.getLogger("AuditLogger")
}
