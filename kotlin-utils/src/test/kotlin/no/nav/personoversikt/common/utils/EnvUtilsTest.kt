package no.nav.personoversikt.common.utils

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class EnvUtilsTest {
    val PROP_KEY = "PROP-KEY"
    val ENV_KEY = "PATH"

    @BeforeEach
    internal fun setUp() {
        System.setProperty(PROP_KEY, "value")
    }

    @AfterEach
    internal fun tearDown() {
        System.clearProperty(PROP_KEY)
    }

    @Test
    internal fun `'getConfig' should look at properties before environment variables`() {
        assertEquals("value", EnvUtils.getConfig(PROP_KEY))
        assertNotEquals("value", EnvUtils.getConfig(ENV_KEY))
        assertTrue((EnvUtils.getConfig(ENV_KEY)?.length ?: 0) > 0)
    }

    @Test
    internal fun `'getConfig' should use defaultValues map if key is not found`() {
        val defaultValues = mapOf<String, String?>(
            "UNKNOWN" to "fallback"
        )
        assertEquals("fallback", EnvUtils.getConfig("UNKNOWN", defaultValues))
    }

    @Test
    internal fun `'getConfig' should return 'null' if key is not found in fallback`() {
        val defaultValues = mapOf<String, String?>(
            "UNKNOWN" to "fallback"
        )
        assertEquals(null, EnvUtils.getConfig("VERY_UNKNOWN", defaultValues))
    }

    @Test
    internal fun `'getRequiredConfig' should throw exception if key is not found in fallback`() {
        val defaultValues = mapOf<String, String?>(
            "UNKNOWN" to "fallback"
        )
        assertThrows<IllegalStateException> {
            EnvUtils.getRequiredConfig("VERY_UNKNOWN", defaultValues)
        }
    }
}
