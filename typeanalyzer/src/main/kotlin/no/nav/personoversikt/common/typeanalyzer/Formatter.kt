package no.nav.personoversikt.common.typeanalyzer

import kotlin.random.Random

interface Format {
    class Type(
        val name: String,
        val fields: List<Field>,
    )

    class Field(
        val name: String,
        val type: String,
        val nullability: Boolean,
    )

    context(StringBuilder)
    fun appendType(type: Type)

    fun fieldType(
        type: CaptureType,
        typeReference: String?,
    ): String
}

object TypescriptFormat : Format {
    context(StringBuilder)
    override fun appendType(type: Format.Type) {
        appendLine("interface ${type.name} {")
        for (field in type.fields) {
            appendLine("  ${field.name}: ${field.type}${if (field.nullability) " | null" else ""},")
        }
        appendLine("}")
    }

    override fun fieldType(
        type: CaptureType,
        typeReference: String?,
    ): String =
        when (type) {
            CaptureType.UNKNOWN -> "any"
            CaptureType.NULL -> "null"
            CaptureType.BOOLEAN -> "boolean"
            CaptureType.INT -> "number"
            CaptureType.DOUBLE -> "number"
            CaptureType.TEXT -> "string"
            CaptureType.LIST -> "Array<${requireNotNull(typeReference)}>"
            CaptureType.OBJECT -> requireNotNull(typeReference)
        }
}

object KotlinFormat : Format {
    context(StringBuilder)
    override fun appendType(type: Format.Type) {
        appendLine("data class ${type.name}(")
        for (field in type.fields) {
            appendLine("    val ${field.name}: ${field.type}${if (field.nullability) "?" else ""},")
        }
        appendLine(")")
    }

    override fun fieldType(
        type: CaptureType,
        typeReference: String?,
    ): String =
        when (type) {
            CaptureType.UNKNOWN -> "Any"
            CaptureType.NULL -> "Unit"
            CaptureType.BOOLEAN -> "Boolean"
            CaptureType.INT -> "Int"
            CaptureType.DOUBLE -> "Double"
            CaptureType.TEXT -> "String"
            CaptureType.LIST -> "List<${requireNotNull(typeReference)}>"
            CaptureType.OBJECT -> requireNotNull(typeReference)
        }
}

class Formatter(
    val format: Format,
    private val rnd: Random = Random.Default,
) {
    val nameMap: MutableMap<ObjectCapture, String> = mutableMapOf()

    fun print(capture: Capture): String {
        val types = capture.findTypes()
        return buildString {
            for (type in types.values) {
                format.appendType(type)
                appendLine()
            }
        }
    }

    private fun Capture.findTypes(current: Map<ObjectCapture, Format.Type> = emptyMap()): Map<ObjectCapture, Format.Type> =
        when (this) {
            is ObjectCapture -> {
                val fieldTypes: Map<ObjectCapture, Format.Type> =
                    this.fields.values
                        .map { it.findTypes() }
                        .reduce { acc, other -> acc.plus(other) }
                current
                    .plus(this to createType(this))
                    .plus(fieldTypes)
            }
            is ListCapture -> current.plus(this.subtype.findTypes())
            else -> current
        }

    private fun createType(capture: ObjectCapture): Format.Type {
        val name =
            nameMap.computeIfAbsent(capture) {
                "Generated_${Integer.toHexString(rnd.nextInt())}"
            }
        return Format.Type(
            name = name,
            fields =
                capture.fields.map { entry ->
                    Format.Field(
                        name = entry.key,
                        type = createTypeName(entry.value),
                        nullability =
                            when (val value = entry.value) {
                                is PrimitiveCapture -> value.nullable
                                is ListCapture -> value.nullable
                                is ObjectCapture -> value.nullable
                                else -> true
                            },
                    )
                },
        )
    }

    private fun createTypeName(capture: Capture): String =
        when (capture) {
            is ListCapture -> format.fieldType(capture.type, createTypeName(capture.subtype))
            is ObjectCapture ->
                nameMap.computeIfAbsent(capture) {
                    "Generated_${Integer.toHexString(rnd.nextInt())}"
                }
            else -> format.fieldType(capture.type, null)
        }
}
