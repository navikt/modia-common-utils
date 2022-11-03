package no.nav.personoversikt.common.test.testenvironment

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TestEnvironmentTest {
    @Test
    internal fun `should setup environment`() {
        TestEnvironment.withEnvironment(mapOf("key1" to "value1")) {
            assertEquals("value1", System.getProperty("key1"))
        }
    }

    @BeforeEach
    internal fun `key should not exist before test execution`() {
        assertEquals(null, System.getProperty("key1"))
    }

    @AfterEach
    internal fun `key should not exist after test execution`() {
        assertEquals(null, System.getProperty("key1"))
    }
}
