package no.nav.personoversikt.common.logging

import no.nav.personoversikt.common.test.logassert.LogAsserts.Companion.captureLogs
import org.junit.jupiter.api.Test
import org.slf4j.event.Level

internal class TjenestekallLoggTest {
    @Test
    internal fun `should expose raw slf4j Logger`() {
        val capture = TjenestekallLogg.raw.captureLogs {
            TjenestekallLogg.raw.info("This is my message")
        }
        capture
            .hasSize(1)
            .logline { messageEquals("This is my message") }
    }

    @Test
    internal fun `should support fields and tags`() {
        val capture = TjenestekallLogg.raw.captureLogs {
            TjenestekallLogg.warn(
                header = "Header",
                fields = mapOf("field_key" to "field_value"),
                tags = mapOf("tag_key" to "tag_value"),
            )
        }

        capture
            .hasSize(1)
            .logline {
                hasLevel(Level.WARN)
                messageContains("Header")
                messageContains("field_key: field_value")
                markerValueEquals("tag_key", "tag_value")
            }
    }

    @Test
    internal fun `should support typed logs`() {
        val capture = TjenestekallLogg.raw.captureLogs {
            TjenestekallLogg.withLogType("TestLogger").warn(
                header = "Header",
                fields = mapOf("field_key" to "field_value"),
                tags = mapOf("tag_key" to "tag_value"),
            )
        }

        capture
            .hasSize(1)
            .logline {
                messageContains("Header")
                messageContains("field_key: field_value")
                markerValueEquals(Logging.LOGTYPE_KEY, "TestLogger")
            }
    }
}
