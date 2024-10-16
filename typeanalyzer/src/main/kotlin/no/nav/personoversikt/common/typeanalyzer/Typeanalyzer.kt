package no.nav.personoversikt.common.typeanalyzer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import no.nav.personoversikt.common.logging.TjenestekallLogg

open class Typeanalyzer {
    private val log = TjenestekallLogg.withLogType("typeanalyzer")
    private val objectMapper =
        JsonMapper
            .builder()
            .findAndAddModules()
            .build()

    private var previousCapture: Capture? = null
    val stats = CaptureStats()

    open fun capture(value: Any?): Capture? {
        objectMapper
            .runCatching { readTree(objectMapper.writeValueAsBytes(value)) }
            .mapCatching { jsonNode -> jsonNode.toCapture() }
            .mapCatching { capture ->
                try {
                    val reconciledCapture = previousCapture?.reconcile(capture) ?: capture
                    stats.capture(changed = reconciledCapture != previousCapture)
                    previousCapture = reconciledCapture
                } catch (throwable: Throwable) {
                    log.error(
                        "Reconciliation failed",
                        mapOf(
                            "exception" to throwable.message,
                            "previousCapture" to previousCapture,
                            "capture" to capture,
                        ),
                        throwable = throwable,
                    )
                    stats.exception(throwable)
                }
            }.onFailure {
                log.error("Reconciliation failed", mapOf("exception" to it.message), throwable = it)
                stats.exception(it)
            }

        return previousCapture
    }

    fun report(): Capture = previousCapture ?: error("No value captured yet")

    fun print(format: Format): String = Formatter(format).print(requireNotNull(previousCapture))

    private fun JsonNode.toCapture(): Capture =
        when (this) {
            is NullNode -> NullCapture
            is IntNode -> PrimitiveCapture(CaptureType.INT, nullable = false)
            is BooleanNode -> PrimitiveCapture(CaptureType.BOOLEAN, nullable = false)
            is DoubleNode -> PrimitiveCapture(CaptureType.DOUBLE, nullable = false)
            is TextNode -> PrimitiveCapture(CaptureType.TEXT, nullable = false)
            is ArrayNode -> {
                if (this.size() == 0) {
                    ListCapture(nullable = false, UnknownCapture)
                } else {
                    val subtype =
                        this
                            .map { it.toCapture() }
                            .reduce { acc, other -> acc.reconcile(other) }
                    ListCapture(nullable = false, subtype)
                }
            }
            is ObjectNode -> {
                val fields = mutableMapOf<String, Capture>()
                this.fields().forEach { entry ->
                    fields[entry.key] = entry.value.toCapture()
                }
                ObjectCapture(nullable = false, fields)
            }
            else -> error("Unknown node-type: ${this::class.simpleName}")
        }
}
