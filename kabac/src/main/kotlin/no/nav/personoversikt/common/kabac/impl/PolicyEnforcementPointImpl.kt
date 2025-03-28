package no.nav.personoversikt.common.kabac.impl

import no.nav.personoversikt.common.kabac.AttributeValue
import no.nav.personoversikt.common.kabac.CombiningAlgorithm
import no.nav.personoversikt.common.kabac.Decision
import no.nav.personoversikt.common.kabac.Kabac

class PolicyEnforcementPointImpl(
    override val bias: Decision.Type = Decision.Type.DENY,
    private val policyDecisionPoint: Kabac.PolicyDecisionPoint,
) : Kabac.PolicyEnforcementPoint {
    init {
        if (bias == Decision.Type.NOT_APPLICABLE) {
            throw UnsupportedOperationException("Bias cannot be 'NOT_APPLICABLE'")
        }
    }

    override fun createEvaluationContext(attributes: List<AttributeValue<*>>): Kabac.EvaluationContext =
        policyDecisionPoint.createEvaluationContext(attributes)

    override fun evaluatePolicies(
        combiningAlgorithm: CombiningAlgorithm,
        bias: Decision.Type,
        attributes: List<AttributeValue<*>>,
        policies: List<Kabac.Policy>,
    ): Decision = evaluatePoliciesWithReport(combiningAlgorithm, bias, attributes, policies).first

    override fun evaluatePoliciesWithReport(
        combiningAlgorithm: CombiningAlgorithm,
        bias: Decision.Type,
        attributes: List<AttributeValue<*>>,
        policies: List<Kabac.Policy>,
    ): Pair<Decision, String> = evaluatePolicyWithReport(bias, attributes, combiningAlgorithm.combine(policies))

    override fun evaluatePolicy(
        bias: Decision.Type,
        attributes: List<AttributeValue<*>>,
        policy: Kabac.Policy,
    ): Decision = evaluatePolicyWithReport(bias, attributes, policy).first

    override fun evaluatePolicyWithReport(
        bias: Decision.Type,
        attributes: List<AttributeValue<*>>,
        policy: Kabac.Policy,
    ): Pair<Decision, String> = evaluatePolicyWithContextWithReport(bias, policyDecisionPoint.createEvaluationContext(attributes), policy)

    override fun evaluatePolicyWithContext(
        bias: Decision.Type,
        ctx: Kabac.EvaluationContext,
        policy: Kabac.Policy,
    ): Decision = evaluatePolicyWithContextWithReport(bias, ctx, policy).first

    override fun evaluatePolicyWithContextWithReport(
        bias: Decision.Type,
        ctx: Kabac.EvaluationContext,
        policy: Kabac.Policy,
    ): Pair<Decision, String> {
        ctx.report(policy.key.name).indent()
        val decision =
            policy
                .evaluate(ctx)
                .withBias(bias)
                .also { ctx.report("Result: $it").unindent() }

        return Pair(decision, ctx.getReport())
    }
}
