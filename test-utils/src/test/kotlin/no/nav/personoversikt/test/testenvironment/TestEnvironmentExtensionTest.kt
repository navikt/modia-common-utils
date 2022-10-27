package no.nav.personoversikt.test.testenvironment

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class TestEnvironmentExtensionTest {
    @RegisterExtension
    @JvmField
    val testEnvironment = TestEnvironmentExtension(
        "key1" to "value1"
    )

    @RegisterExtension
    @JvmField
    val lazyEnvironment = TestEnvironmentExtension {
        mapOf(
            "key2" to "value2"
        )
    }

    @Test
    internal fun `should setup environment`() {
        assertEquals("value1", System.getProperty("key1"))
        assertEquals("value2", System.getProperty("key2"))
    }

    @BeforeEach
    internal fun `key should not exist before test execution`() {
        assertEquals(null, System.getProperty("key2"))
    }

    @AfterEach
    internal fun `key should not exist after test execution`() {
        assertEquals(null, System.getProperty("key2"))
    }
}
