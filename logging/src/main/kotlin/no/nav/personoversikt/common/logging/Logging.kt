package no.nav.personoversikt.common.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Logging {
    const val LOGTYPE_KEY = "logtype"
    const val TEAM_LOGS_MARKER = "TEAM_LOGS"
    val secureLog: Logger = LoggerFactory.getLogger("SecureLog")
    val auditLog: Logger = LoggerFactory.getLogger("AuditLogger")
    val teamLog: Logger = LoggerFactory.getLogger("TeamLog")
}
