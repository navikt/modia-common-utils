package no.nav.personoversikt.kabac

import no.nav.personoversikt.kabac.impl.EvaluationContextImpl
import no.nav.personoversikt.kabac.utils.Key
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail

object KabacTestUtils {
    fun createTestPolicy(block: (ctx: Kabac.EvaluationContext) -> Decision) = object : Kabac.Policy {
        override val key = Key<Kabac.Policy>("test-policy")
        override fun evaluate(ctx: Kabac.EvaluationContext) = block(ctx)
    }

    class PolicyTester(private val policy: Kabac.Policy) {
        fun assertPermit(vararg attributes: Kabac.PolicyInformationPoint<*>) {
            val ctx = EvaluationContextImpl(*attributes)
            val decision = try {
                ctx.report(policy.key.name).indent()
                policy.evaluate(ctx)
            } catch (e: Throwable) {
                fail("Policy evaluation should not throw exception", e)
            } finally {
                ctx.unindent()
            }
            assertEquals(Decision.Permit::class, decision::class)
            assertEquals(Decision.Permit(), decision)
        }

        fun assertDeny(vararg attributes: Kabac.PolicyInformationPoint<*>): MessageAsserter {
            val ctx = EvaluationContextImpl(*attributes)
            val decision = try {
                ctx.report(policy.key.name).indent()
                policy.evaluate(ctx)
            } catch (e: Throwable) {
                fail("Policy evaluation should not throw exception", e)
            } finally {
                ctx.unindent()
            }
            assertEquals(Decision.Deny::class, decision::class)
            return MessageAsserter(decision)
        }

        fun assertNotApplicable(vararg attributes: Kabac.PolicyInformationPoint<*>): MessageAsserter {
            val ctx = EvaluationContextImpl(*attributes)
            val decision = try {
                policy.evaluate(ctx)
            } catch (e: Throwable) {
                fail("Policy evaluation should not throw exception", e)
            }
            assertEquals(Decision.NotApplicable::class, decision::class)
            return MessageAsserter(decision)
        }

        class MessageAsserter(private val decision: Decision) {
            fun withMessage(expectedMessage: String) {
                val actualMessage = when (decision) {
                    is Decision.Deny -> decision.message
                    is Decision.NotApplicable -> decision.message
                    else -> null
                }
                assertEquals(expectedMessage, actualMessage)
            }
        }
    }
}
