package no.nav.personoversikt.common.test.testenvironment

import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class TestEnvironmentRuleTest {
    @Rule
    @JvmField
    val testenvironment =
        TestEnvironmentRule(
            "key1" to "value1",
        )

    @Rule
    @JvmField
    val lazyenvironment =
        TestEnvironmentRule {
            mapOf(
                "key2" to "value2",
            )
        }

    @Test
    internal fun `should setup environment`() {
        assertEquals("value1", System.getProperty("key1"))
        assertEquals("value2", System.getProperty("key2"))
    }

    companion object {
        @JvmStatic
        @BeforeClass
        internal fun `key should not exist before test execution`() {
            assertEquals(null, System.getProperty("key1"))
            assertEquals(null, System.getProperty("key2"))
        }

        @JvmStatic
        @AfterClass
        internal fun `key should not exist after test execution`() {
            assertEquals(null, System.getProperty("key1"))
            assertEquals(null, System.getProperty("key2"))
        }
    }
}
