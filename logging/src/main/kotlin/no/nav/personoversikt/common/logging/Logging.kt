package no.nav.personoversikt.common.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Logging {
    const val LOGTYPE_KEY = "logtype"
    val secureLog: Logger = LoggerFactory.getLogger("SecureLog")
    val auditLog: Logger = LoggerFactory.getLogger("AuditLogger")
}
