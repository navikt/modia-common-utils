package no.nav.personoversikt.common.kabac.impl

import no.nav.personoversikt.common.kabac.AttributeValue
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.utils.Key

class PolicyDecisionPointImpl : Kabac.PolicyDecisionPoint {
    private val providerRegister = mutableMapOf<Key<*>, Kabac.PolicyInformationPoint<*>>()

    override fun install(informationPoint: Kabac.PolicyInformationPoint<*>): Kabac.PolicyDecisionPoint {
        providerRegister[informationPoint.key] = informationPoint
        return this
    }

    override fun createEvaluationContext(attributes: List<AttributeValue<*>>): Kabac.EvaluationContext =
        EvaluationContextImpl(providerRegister.values + attributes)
}
