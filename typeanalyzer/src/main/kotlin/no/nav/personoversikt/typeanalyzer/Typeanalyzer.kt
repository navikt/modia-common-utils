package no.nav.personoversikt.typeanalyzer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory

class Typeanalyzer {
    private val log = LoggerFactory.getLogger(Typeanalyzer::class.java)
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())

    private var previousCapture: Capture? = null
    private var previousJsonNode: JsonNode? = null

    fun capture(value: Any?): Capture? {
        objectMapper
            .runCatching { readTree(objectMapper.writeValueAsBytes(value)) }
            .mapCatching { jsonNode ->
                try {
                    val capture = jsonNode.toCapture()
                    previousCapture = previousCapture?.reconcile(capture) ?: capture
                } catch (err: Throwable) {
                    log.error(
                        """
                        Reconciliation failed.
                        Previous: $previousJsonNode
                        Capture: $jsonNode
                        """.trimIndent(),
                        err
                    )
                    throw err
                }
                previousJsonNode = jsonNode
            }

        return previousCapture
    }

    fun report(): Capture {
        return previousCapture ?: error("No value captured yet")
    }

    fun print(format: Format): String = Formatter(format).print(requireNotNull(previousCapture))

    private fun JsonNode.toCapture(): Capture {
        return when (this) {
            is NullNode -> NullCapture
            is IntNode -> PrimitiveCapture(CaptureType.INT, nullable = false)
            is BooleanNode -> PrimitiveCapture(CaptureType.BOOLEAN, nullable = false)
            is DoubleNode -> PrimitiveCapture(CaptureType.DOUBLE, nullable = false)
            is TextNode -> PrimitiveCapture(CaptureType.TEXT, nullable = false)
            is ArrayNode -> {
                if (this.size() == 0) {
                    ListCapture(nullable = false, UnknownCapture)
                } else {
                    val subtype = this
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
}
