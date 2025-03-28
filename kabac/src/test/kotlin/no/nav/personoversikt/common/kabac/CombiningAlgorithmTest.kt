package no.nav.personoversikt.common.kabac

import no.nav.personoversikt.common.kabac.KabacTestUtils.createTestPolicy
import no.nav.personoversikt.common.kabac.impl.EvaluationContextImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class CombiningAlgorithmTest {
    private val evaluationContextImpl = EvaluationContextImpl(emptyList())

    companion object {
        private val permitPolicy = createTestPolicy { Decision.Permit() }.named("PERMIT")
        private val denyPolicy = createTestPolicy { Decision.Deny("Deny", Decision.NO_APPLICABLE_POLICY_FOUND) }.named("DENY")
        private val notApplicablePolicy = createTestPolicy { Decision.NotApplicable("No applicable") }.named("NOT_APPLICABLE")

        private val permitOverride = CombiningAlgorithm.permitOverride.named("PermitOverride")
        private val denyOverride = CombiningAlgorithm.denyOverride.named("DenyOverride")
        private val firstApplicable = CombiningAlgorithm.firstApplicable.named("FirstApplicable")

        @JvmStatic
        fun singlePolicyTests(): List<Arguments> =
            listOf(
                forPolicies(permitPolicy).expectResults(
                    permitOverride to Decision.Type.PERMIT,
                    denyOverride to Decision.Type.PERMIT,
                    firstApplicable to Decision.Type.PERMIT,
                ),
                forPolicies(denyPolicy).expectResults(
                    permitOverride to Decision.Type.DENY,
                    denyOverride to Decision.Type.DENY,
                    firstApplicable to Decision.Type.DENY,
                ),
                forPolicies(notApplicablePolicy).expectResults(
                    permitOverride to Decision.Type.NOT_APPLICABLE,
                    denyOverride to Decision.Type.NOT_APPLICABLE,
                    firstApplicable to Decision.Type.NOT_APPLICABLE,
                ),
            ).flatten()

        @JvmStatic
        fun combinationTests(): List<Arguments> =
            listOf(
                forPolicies(permitPolicy, permitPolicy).expectResults(
                    permitOverride to Decision.Type.PERMIT,
                    denyOverride to Decision.Type.PERMIT,
                    firstApplicable to Decision.Type.PERMIT,
                ),
                forPolicies(permitPolicy, denyPolicy).expectResults(
                    permitOverride to Decision.Type.PERMIT,
                    denyOverride to Decision.Type.DENY,
                    firstApplicable to Decision.Type.PERMIT,
                ),
                forPolicies(denyPolicy, permitPolicy).expectResults(
                    permitOverride to Decision.Type.PERMIT,
                    denyOverride to Decision.Type.DENY,
                    firstApplicable to Decision.Type.DENY,
                ),
                forPolicies(permitPolicy, notApplicablePolicy).expectResults(
                    permitOverride to Decision.Type.PERMIT,
                    denyOverride to Decision.Type.PERMIT,
                    firstApplicable to Decision.Type.PERMIT,
                ),
                forPolicies(notApplicablePolicy, permitPolicy).expectResults(
                    permitOverride to Decision.Type.PERMIT,
                    denyOverride to Decision.Type.PERMIT,
                    firstApplicable to Decision.Type.PERMIT,
                ),
                forPolicies(notApplicablePolicy, denyPolicy).expectResults(
                    permitOverride to Decision.Type.DENY,
                    denyOverride to Decision.Type.DENY,
                    firstApplicable to Decision.Type.DENY,
                ),
                forPolicies(denyPolicy, notApplicablePolicy).expectResults(
                    permitOverride to Decision.Type.DENY,
                    denyOverride to Decision.Type.DENY,
                    firstApplicable to Decision.Type.DENY,
                ),
                forPolicies(denyPolicy, denyPolicy).expectResults(
                    permitOverride to Decision.Type.DENY,
                    denyOverride to Decision.Type.DENY,
                    firstApplicable to Decision.Type.DENY,
                ),
                forPolicies(notApplicablePolicy, notApplicablePolicy).expectResults(
                    permitOverride to Decision.Type.NOT_APPLICABLE,
                    denyOverride to Decision.Type.NOT_APPLICABLE,
                    firstApplicable to Decision.Type.NOT_APPLICABLE,
                ),
            ).flatten()
    }

    @ParameterizedTest
    @MethodSource("singlePolicyTests")
    fun `single policy tests`(
        algorithm: CombiningAlgorithm,
        policies: List<Kabac.Policy>,
        expected: Decision.Type,
    ) {
        val decision = algorithm.combine(policies).evaluate(evaluationContextImpl)
        assertEquals(expected, decision.type)
    }

    @ParameterizedTest
    @MethodSource("combinationTests")
    fun `multiple policies test`(
        algorithm: CombiningAlgorithm,
        policies: List<Kabac.Policy>,
        expected: Decision.Type,
    ) {
        val decision = algorithm.combine(policies).evaluate(evaluationContextImpl)
        assertEquals(expected, decision.type)
    }
}

private fun forPolicies(vararg elements: Named<Kabac.Policy>): List<Named<Kabac.Policy>> =
    if (elements.isNotEmpty()) elements.asList() else emptyList()

private fun <T : CombiningAlgorithm> T.named(name: String) = Named.named(name, this)

private fun Kabac.Policy.named(name: String) = Named.named(name, this)

private fun List<Named<Kabac.Policy>>.named() = Named.named("[${this.joinToString(",") { it.name }}]", this.map { it.payload })

private fun List<Named<Kabac.Policy>>.expectResults(vararg results: Pair<Named<CombiningAlgorithm>, Decision.Type>): List<Arguments> =
    results.map { result ->
        Arguments.of(result.first, this.named(), result.second)
    }
