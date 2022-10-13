package no.nav.personoversikt.test.testenvironment

import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class TestEnvironmentRuleTest {
    @Rule
    @JvmField
    val testenvironment = TestEnvironmentRule(
        "key1" to "value1"
    )

    @Test
    internal fun `should setup environment`() {
        assertEquals("value1", System.getProperty("key1"))
    }

    companion object {
        @JvmStatic
        @BeforeClass
        internal fun `key should not exist before test execution`() {
            assertEquals(null, System.getProperty("key1"))
        }

        @JvmStatic
        @AfterClass
        internal fun `key should not exist after test execution`() {
            assertEquals(null, System.getProperty("key1"))
        }
    }
}
