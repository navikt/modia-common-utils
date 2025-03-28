package no.nav.personoversikt.common.kabac.impl

import no.nav.personoversikt.common.kabac.Kabac

class EvaluationReporterImpl : Kabac.EvaluationReporter {
    private val sb = StringBuilder()
    private var indent = ""

    override fun report(message: String): Kabac.EvaluationReporter =
        this
            .also { sb.append(indent).appendLine(message) }

    override fun indent(): Kabac.EvaluationReporter =
        this
            .also { indent = "$indent\t" }

    override fun unindent(): Kabac.EvaluationReporter =
        this
            .also { indent = indent.removePrefix("\t") }

    override fun getReport(): String = sb.toString()
}
