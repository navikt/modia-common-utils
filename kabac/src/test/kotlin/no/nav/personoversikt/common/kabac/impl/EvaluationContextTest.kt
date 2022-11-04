package no.nav.personoversikt.common.kabac.impl

import no.nav.personoversikt.common.kabac.AttributeValue
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.KabacException
import no.nav.personoversikt.common.kabac.utils.Key
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class EvaluationContextTest {
    private val dummyKey = Key<String>("dummy")
    private val nullableKey = Key<String?>("dummy")
    private val dummyProvider = AttributeValue(dummyKey, "dummy-value")
    private val nullProvider = AttributeValue(nullableKey, null)

    @Test
    internal fun `get value based on providerkey`() {
        val ctx = EvaluationContextImpl(listOf(dummyProvider))

        val value = ctx.getValue(dummyKey)

        assertEquals("dummy-value", value)
    }

    @Test
    internal fun `get value based on provide`() {
        val ctx = EvaluationContextImpl(listOf(dummyProvider))

        val value = ctx.getValue(dummyProvider)

        assertEquals("dummy-value", value)
    }

    @Test
    internal fun `require value based on key`() {
        val ctx = EvaluationContextImpl(listOf(dummyProvider))

        val value = ctx.getValue(dummyKey)

        assertEquals("dummy-value", value)
    }

    @Test
    internal fun `require value based on provider`() {
        val ctx = EvaluationContextImpl(listOf(dummyProvider))

        val value = ctx.getValue(dummyProvider)

        assertEquals("dummy-value", value)
    }

    @Test
    internal fun `get nullable value should return null`() {
        val ctx = EvaluationContextImpl(listOf(nullProvider))

        val value = ctx.getValue(nullProvider)

        assertNull(value)
    }

    @Test
    internal fun `require nullable value should throw exception`() {
        val ctx = EvaluationContextImpl(listOf(nullProvider))

        assertNull(ctx.getValue(nullProvider))
    }

    @Test
    internal fun `getting value from non-configured provider should throw exception`() {
        val ctx = EvaluationContextImpl(emptyList())

        assertThrows<KabacException.MissingPolicyInformationPointException> {
            ctx.getValue(dummyProvider)
        }
    }

    @Test
    internal fun `values retrived from providers should be cached`() {
        val fastMockProvider = object : Kabac.PolicyInformationPoint<String> {
            var executionCount: Int = 0
            override val key = Key<String>("mock-key")
            override fun provide(ctx: Kabac.EvaluationContext): String {
                executionCount++
                return "mock-value"
            }
        }
        val ctx = EvaluationContextImpl(listOf(fastMockProvider))

        val values = listOf(
            ctx.getValue(fastMockProvider),
            ctx.getValue(fastMockProvider)
        )

        assertEquals(listOf("mock-value", "mock-value"), values)
        assertEquals(1, fastMockProvider.executionCount)
    }

    @Test
    internal fun `should throw error if cyclic pip usage is found`() {
        val size = 4
        val keys = mutableListOf<Key<Any>>()
        val providers = mutableListOf<Kabac.PolicyInformationPoint<Any>>()
        repeat(size) { i -> keys.add(Key("key${i + 1}")) }
        repeat(size) { i -> providers.add(createCyclicProvider(keys[i], keys[(i + 1) % size])) }
        val ctx = EvaluationContextImpl(providers)

        val exception = assertThrows<KabacException.CyclicDependenciesException> {
            ctx.getValue(keys[0])
        }
        assertEquals("Cycle: key1 -> key2 -> key3 -> key4 -> key1", exception.message)
    }

    @Test
    internal fun `self referencing pip should throw error`() {
        val size = 1
        val keys = mutableListOf<Key<Any>>()
        repeat(size) { i -> keys.add(Key("key${i + 1}")) }
        val providers = mutableListOf<Kabac.PolicyInformationPoint<Any>>(
            createCyclicProvider(keys[0], keys[0])
        )
        val ctx = EvaluationContextImpl(providers)

        val exception = assertThrows<KabacException.CyclicDependenciesException> {
            ctx.getValue(keys[0])
        }

        assertEquals("Cycle: key1 -> key1", exception.message)
    }

    @Test
    internal fun `break cyclic pip usage by providing a single value`() {
        val size = 10
        val keys = mutableListOf<Key<Any>>()
        val providers = mutableListOf<Kabac.PolicyInformationPoint<Any>>()
        repeat(size) { i -> keys.add(Key("key${i + 1}")) }
        repeat(size) { i -> providers.add(createCyclicProvider(keys[i], keys[(i + 1) % size])) }

        val providedKey = keys[5]
        val ctx = EvaluationContextImpl(
            providers + listOf(
                AttributeValue(providedKey, "OK")
            )
        )

        val result = ctx.getValue(keys[0])

        assertEquals("OK", result)
    }

    @Test
    internal fun `should merge contexts`() {
        val policyEnforcementPointImpl = PolicyEnforcementPointImpl(policyDecisionPoint = PolicyDecisionPointImpl())
        val firstKey = Key<String>("first")
        val firstValue = AttributeValue(firstKey, "first-value")
        val secondKey = Key<String>("second")
        val secondValue = AttributeValue(secondKey, "second-value")

        val firstCtx = policyEnforcementPointImpl.createEvaluationContext(listOf(firstValue)).also {
            it.getValue(firstKey)
        }
        val secondCtx = policyEnforcementPointImpl.createEvaluationContext(listOf(secondValue))

        val mergedCtx = firstCtx + secondCtx
        mergedCtx as EvaluationContextImpl

        assertEquals(1, mergedCtx.cache.size)
        assertEquals(2, mergedCtx.register.size)
    }

    private fun <T> createCyclicProvider(key: Key<T>, dependent: Key<T>) = object : Kabac.PolicyInformationPoint<T> {
        override val key: Key<T> = key
        override fun provide(ctx: Kabac.EvaluationContext): T {
            return ctx.getValue(dependent)
        }
    }
}
