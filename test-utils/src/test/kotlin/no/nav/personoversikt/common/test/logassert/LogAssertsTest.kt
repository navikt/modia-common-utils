package no.nav.personoversikt.common.test.logassert

import no.nav.personoversikt.common.test.logassert.LogAsserts.Companion.captureLogs
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import org.slf4j.helpers.BasicMarkerFactory

internal class LogAssertsTest {
    private val logger = LoggerFactory.getLogger(LogAsserts::class.java)

    @Test
    internal fun `should capture log calls`() {
        val captured = logger.captureLogs {
            logger.info("Log this message")
            logger.warn("Log another message")
            logger.error("Log third message")
        }

        captured
            .hasSize(3)
            .logline {
                hasLevel(Level.INFO)
                messageEquals("Log this message")
            }
            .logline {
                hasLevel(Level.WARN)
                messageContains("another")
            }
            .logline {
                hasLevel(Level.ERROR)
            }
    }

    @Test
    internal fun `should capture markers`() {
        val captured = logger.captureLogs {
            logger.info(
                BasicMarkerFactory().getMarker("my-marker"),
                "My message"
            )
        }

        captured
            .hasSize(1)
            .logline {
                messageEquals("My message")
                hasMarker("my-marker")
            }
    }
}
