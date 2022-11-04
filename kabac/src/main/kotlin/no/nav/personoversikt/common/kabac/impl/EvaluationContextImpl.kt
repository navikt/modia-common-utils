package no.nav.personoversikt.common.kabac.impl

import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.KabacException
import no.nav.personoversikt.common.kabac.utils.Key
import no.nav.personoversikt.common.kabac.utils.KeyStack

class EvaluationContextImpl(
    private val providers: List<Kabac.PolicyInformationPoint<*>>
) : Kabac.EvaluationContext, Kabac.EvaluationReporter by EvaluationReporterImpl() {
    internal val register = providers.associateBy { it.key }
    internal val cache = mutableMapOf<Key<*>, Any?>()
    private val keystack = KeyStack()

    override fun <TValue> getValue(attributeKey: Kabac.AttributeKey<TValue>): TValue = getValue(attributeKey.key)

    @Suppress("UNCHECKED_CAST")
    override fun <TValue> getValue(key: Key<TValue>): TValue {
        return keystack.withCycleDetection(key) {
            if (cache.containsKey(key)) {
                val value = cache[key] as TValue
                report("Requested $key, cache-hit")
                value
            } else {
                val provider = register[key]
                if (provider == null) {
                    report("Requested $key, no provider found")
                    throw KabacException.MissingPolicyInformationPointException("Could not find provider for $key")
                }

                val value = provider.provide(this) as TValue
                report("Request $key, cache-miss: $value")
                cache[key] = value
                value
            }
        }
    }

    override fun plus(other: Kabac.EvaluationContext): Kabac.EvaluationContext {
        return when (other) {
            is EvaluationContextImpl -> EvaluationContextImpl(providers + other.providers)
                .also { it.cache.putAll(cache + other.cache) }
            else -> error("Can not merge evaluation context of different type")
        }
    }

    companion object {
        operator fun invoke(vararg providers: Kabac.PolicyInformationPoint<*>) = EvaluationContextImpl(providers.toList())
    }
}
