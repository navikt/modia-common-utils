package no.nav.personoversikt.common.typeanalyzer

sealed class Capture(open var type: CaptureType)
object UnknownCapture : Capture(CaptureType.UNKNOWN)
object NullCapture : Capture(CaptureType.NULL)
data class PrimitiveCapture(override var type: CaptureType, var nullable: Boolean) : Capture(type)
data class ListCapture(var nullable: Boolean, var subtype: Capture) : Capture(CaptureType.LIST)
data class ObjectCapture(
    var nullable: Boolean,
    var fields: Map<String, Capture>
) : Capture(CaptureType.OBJECT)

fun Capture.reconcile(other: Capture): Capture {
    return if (this.type != other.type) {
        when {
            this == UnknownCapture -> other
            other == UnknownCapture -> this
            this == NullCapture && other is PrimitiveCapture -> other.copy(nullable = true)
            this == NullCapture && other is ListCapture -> other.copy(nullable = true)
            this == NullCapture && other is ObjectCapture -> other.copy(nullable = true)
            this is PrimitiveCapture && other == NullCapture -> this.copy(nullable = true)
            this is ListCapture && other == NullCapture -> this.copy(nullable = true)
            this is ObjectCapture && other == NullCapture -> this.copy(nullable = true)
            else -> error(
                """
                    Type mismatch, and could not reconcile typoes. Expected type ${this.type}, but got ${other.type}
                    Base: $this
                    Other: $other
                """.trimIndent()
            )
        }
    } else {
        when (this) {
            is PrimitiveCapture -> this.copy(nullable = this.nullable or (other as PrimitiveCapture).nullable)
            is ListCapture -> {
                (other as ListCapture)
                this.copy(
                    nullable = this.nullable or other.nullable,
                    subtype = this.subtype.reconcile(other.subtype)
                )
            }
            is ObjectCapture -> {
                (other as ObjectCapture)
                val commonkeys = (this.fields.keys.intersect(other.fields.keys)).associateWith { key ->
                    requireNotNull(this.fields[key]).reconcile(requireNotNull(other.fields[key]))
                }
                val newkeys = (other.fields.keys - this.fields.keys).associateWith { key ->
                    when (val field = requireNotNull(other.fields[key])) {
                        is PrimitiveCapture -> field.copy(nullable = true)
                        is ListCapture -> field.copy(nullable = true)
                        is ObjectCapture -> field.copy(nullable = true)
                        else -> field
                    }
                }
                val missingkeys = (this.fields.keys - other.fields.keys).associateWith { key ->
                    when (val field = requireNotNull(this.fields[key])) {
                        is PrimitiveCapture -> field.copy(nullable = true)
                        is ListCapture -> field.copy(nullable = true)
                        is ObjectCapture -> field.copy(nullable = true)
                        else -> field
                    }
                }

                this.copy(
                    nullable = this.nullable or other.nullable,
                    fields = commonkeys + newkeys + missingkeys
                )
            }
            else -> this
        }
    }
}
