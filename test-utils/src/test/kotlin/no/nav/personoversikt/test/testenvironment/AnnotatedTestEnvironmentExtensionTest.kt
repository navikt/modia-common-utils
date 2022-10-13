package no.nav.personoversikt.test.testenvironment

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestEnvironmentExtension::class)
class AnnotatedTestEnvironmentExtensionTest(val testEnvironmentExtension: TestEnvironmentExtension) {
    @Test
    internal fun `should setup environment`() {
        testEnvironmentExtension.runWithEnvironment(mapOf("key1" to "value1")) {
            Assertions.assertEquals("value1", System.getProperty("key1"))
        }
    }

    @BeforeEach
    internal fun `key should not exist before test execution`() {
        Assertions.assertEquals(null, System.getProperty("key1"))
    }

    @AfterEach
    internal fun `key should not exist after test execution`() {
        Assertions.assertEquals(null, System.getProperty("key1"))
    }
}
