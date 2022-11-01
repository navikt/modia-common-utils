package no.nav.personoversikt.kabac

import no.nav.personoversikt.kabac.KabacTestUtils.createTestPolicy
import no.nav.personoversikt.kabac.impl.PolicyDecisionPointImpl
import no.nav.personoversikt.kabac.impl.PolicyEnforcementPointImpl
import no.nav.personoversikt.kabac.utils.Key
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class KabacTest {
    object DummyProvider : Kabac.PolicyInformationPoint<String> {
        override val key: Key<String> = Key("dummy-provider")
        override fun provide(ctx: Kabac.EvaluationContext): String {
            return "dummy value"
        }
    }

    object DummyDependentProvider : Kabac.PolicyInformationPoint<Int> {
        override val key: Key<Int> = Key("dummy-dependent-provider")
        override fun provide(ctx: Kabac.EvaluationContext): Int {
            return ctx.getValue(DummyProvider).length
        }
    }

    object ErrorThrowingProvider : Kabac.PolicyInformationPoint<String> {
        override val key: Key<String> = Key("error-throwing-provider")
        override fun provide(ctx: Kabac.EvaluationContext): String {
            throw IllegalArgumentException("Something went wrong")
        }
    }

    @Test
    internal fun `installing provider`() {
        val kabac = createPEP(DummyProvider)

        val decision: Decision = kabac.evaluatePolicy(
            policy = createTestPolicy { ctx ->
                Decision.Deny(ctx.getValue(DummyProvider), Decision.NO_APPLICABLE_POLICY_FOUND)
            }
        )

        assertEquals(Decision.Deny("dummy value", Decision.NO_APPLICABLE_POLICY_FOUND), decision)
    }

    @Test
    internal fun `missing attribute should cause error`() {
        val kabac = createPEP(DummyDependentProvider)

        val exception = assertThrows<KabacException.MissingPolicyInformationPointException> {
            kabac.evaluatePolicy(
                policy = createTestPolicy { ctx ->
                    Decision.Deny(ctx.getValue(DummyProvider), Decision.NO_APPLICABLE_POLICY_FOUND)
                }
            )
        }

        assertEquals("Could not find provider for Key(dummy-provider)", exception.message)
    }

    @Test
    internal fun `dependent provider should get its value from another provider`() {
        val kabac = createPEP(DummyProvider, DummyDependentProvider)

        val decision: Decision = kabac.evaluatePolicy(
            policy = createTestPolicy { ctx ->
                val value: Int = ctx.getValue(DummyDependentProvider)
                Decision.Deny("Length of string was: $value", Decision.NO_APPLICABLE_POLICY_FOUND)
            }
        )

        assertEquals(Decision.Deny("Length of string was: 11", Decision.NO_APPLICABLE_POLICY_FOUND), decision)
    }

    @Test
    internal fun `providing attribute value directly should short-circuit provider chain even if it exists`() {
        val kabac = createPEP(DummyProvider, DummyDependentProvider)

        val decision: Decision = kabac.evaluatePolicy(
            attributes = listOf(
                AttributeValue(DummyProvider, "this is a longer value")
            ),
            policy = createTestPolicy { ctx ->
                val value: Int = ctx.getValue(DummyDependentProvider)
                Decision.Deny("Length of string was: $value", Decision.NO_APPLICABLE_POLICY_FOUND)
            }
        )

        assertEquals(Decision.Deny("Length of string was: 22", Decision.NO_APPLICABLE_POLICY_FOUND), decision)
    }

    @Test
    internal fun `providing attribute value directly should short-circuit provider chain`() {
        val kabac = createPEP(DummyDependentProvider)

        val decision: Decision = kabac.evaluatePolicy(
            attributes = listOf(
                AttributeValue(DummyProvider, "this is a longer value")
            ),
            policy = createTestPolicy { ctx ->
                val value: Int = ctx.getValue(DummyDependentProvider)
                Decision.Deny("Length of string was: $value", Decision.NO_APPLICABLE_POLICY_FOUND)
            }
        )

        assertEquals(Decision.Deny("Length of string was: 22", Decision.NO_APPLICABLE_POLICY_FOUND), decision)
    }

    @Test
    internal fun `provider throwing error should bubble up`() {
        val kabac = createPEP(ErrorThrowingProvider)

        val exception = assertThrows<IllegalArgumentException> {
            kabac.evaluatePolicy(
                policy = createTestPolicy { ctx ->
                    Decision.Deny(ctx.getValue(ErrorThrowingProvider), Decision.NO_APPLICABLE_POLICY_FOUND)
                }
            )
        }

        assertEquals("Something went wrong", exception.message)
    }

    @Test
    internal fun `kabac bias should be applied to decision`() {
        val kabac = PolicyEnforcementPointImpl(bias = Decision.Type.PERMIT, PolicyDecisionPointImpl())

        val decision = kabac.evaluatePolicy(
            policy = createTestPolicy {
                Decision.NotApplicable("Doesn't matter")
            }
        )

        assertEquals(Decision.Permit(), decision)
    }

    @Test
    internal fun `policyevaluation bias should override kabac bias`() {
        val kabac = PolicyEnforcementPointImpl(bias = Decision.Type.PERMIT, PolicyDecisionPointImpl())

        val decision = kabac.evaluatePolicy(
            bias = Decision.Type.DENY,
            policy = createTestPolicy {
                Decision.NotApplicable("Doesn't matter")
            }
        )

        assertEquals(Decision.Deny("No applicable policy found", Decision.NO_APPLICABLE_POLICY_FOUND), decision)
    }

    @Test
    internal fun `not_applicable cannot be set as bias`() {
        val exception = assertThrows<UnsupportedOperationException> {
            PolicyEnforcementPointImpl(bias = Decision.Type.NOT_APPLICABLE, PolicyDecisionPointImpl())
        }

        assertEquals("Bias cannot be 'NOT_APPLICABLE'", exception.message)
    }

    @Test
    internal fun `supplied attributes override registered providers`() {
        val kabac = createPEP(DummyProvider)

        val decision = kabac.evaluatePolicy(
            attributes = listOf(AttributeValue(DummyProvider, "overridden")),
            policy = createTestPolicy { ctx ->
                Decision.Deny(ctx.getValue(DummyProvider), Decision.NO_APPLICABLE_POLICY_FOUND)
            }
        )

        assertEquals(Decision.Deny("overridden", Decision.NO_APPLICABLE_POLICY_FOUND), decision)
    }

    private fun createPEP(vararg pips: Kabac.PolicyInformationPoint<*>): Kabac.PolicyEnforcementPoint {
        return PolicyEnforcementPointImpl(
            policyDecisionPoint = PolicyDecisionPointImpl().apply {
                pips.forEach(::install)
            }
        )
    }
}
