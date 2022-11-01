package no.nav.personoversikt.kabac.utils

import no.nav.personoversikt.kabac.AttributeValue

class Key<TYPE>(val name: String) {
    init {
        if (name.isEmpty()) {
            throw IllegalStateException("Key name cannot be empty")
        }
    }

    fun withValue(value: TYPE) = AttributeValue(this, value)

    override fun toString(): String = "Key($name)"
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = name == other

    companion object {
        operator fun <T> invoke(any: Any): Key<T> = Key(any::class.java.simpleName)
    }
}
