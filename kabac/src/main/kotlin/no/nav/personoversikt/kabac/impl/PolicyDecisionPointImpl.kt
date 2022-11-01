package no.nav.personoversikt.kabac.impl

import no.nav.personoversikt.kabac.AttributeValue
import no.nav.personoversikt.kabac.Kabac
import no.nav.personoversikt.kabac.utils.Key

class PolicyDecisionPointImpl : Kabac.PolicyDecisionPoint {
    private val providerRegister = mutableMapOf<Key<*>, Kabac.PolicyInformationPoint<*>>()

    override fun install(informationPoint: Kabac.PolicyInformationPoint<*>): Kabac.PolicyDecisionPoint {
        providerRegister[informationPoint.key] = informationPoint
        return this
    }

    override fun createEvaluationContext(attributes: List<AttributeValue<*>>): Kabac.EvaluationContext {
        return EvaluationContextImpl(providerRegister.values + attributes)
    }
}
