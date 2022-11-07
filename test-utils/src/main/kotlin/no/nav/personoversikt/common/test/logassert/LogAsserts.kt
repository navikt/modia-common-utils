package no.nav.personoversikt.common.test.logassert

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate
import net.logstash.logback.argument.StructuredArgument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import ch.qos.logback.classic.Logger as LogbackLogger
import org.slf4j.Logger as Logger

class LogAsserts(private val appender: ListAppender<ILoggingEvent>) {
    companion object {
        fun captureLogs(block: () -> Unit): LogAsserts {
            return LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).captureLogs(block)
        }

        fun Logger.captureLogs(block: () -> Unit): LogAsserts {
            this as LogbackLogger
            val appender = ListAppender<ILoggingEvent>().apply { start() }
            this.addAppender(appender)
            block()
            this.detachAppender(appender)
            appender.stop()
            return LogAsserts(appender)
        }
    }

    private var eventIndex: Int = 0

    fun hasSize(expectedSize: Int): LogAsserts {
        assertEquals(expectedSize, appender.list.size, "Number of log messages did not match")
        return this
    }

    fun logline(block: MessageAsserter.() -> Unit): LogAsserts {
        MessageAsserter(appender.list[eventIndex++]).apply(block)
        return this
    }

    fun logline(index: Int, block: MessageAsserter.() -> Unit): LogAsserts {
        MessageAsserter(appender.list[index]).apply(block)
        return this
    }

    fun skipline(): LogAsserts {
        eventIndex++
        return this
    }

    class MessageAsserter(private val event: ILoggingEvent) {
        private val markerMap: Map<String, Any?> by lazy {
            val map = mutableMapOf<String, Any?>()
            val mapCapture = object : JsonGeneratorDelegate(null) {
                lateinit var fieldname: String
                override fun writeFieldName(name: String) {
                    fieldname = name
                }
                override fun writeObject(value: Any?) {
                    map[fieldname] = value
                }
            }
            if (event.marker is StructuredArgument) {
                (event.marker as StructuredArgument).writeTo(mapCapture)
            } else {
                map[event.marker.name] = null
                for (marker in event.marker) {
                    map[marker.name] = null
                }
            }
            map
        }

        fun hasLevel(level: Level) {
            assertEquals(level.toString(), event.level.toString(), "Level did not match")
        }

        fun messageEquals(expected: String) {
            assertEquals(expected, event.message)
        }

        fun messageContains(expected: String) {
            assertTrue(event.message.contains(expected))
        }

        fun hasMarker(markername: String) {
            assertTrue(markerMap.containsKey(markername), "Could not find marker")
        }

        fun markerValueEquals(markername: String, expectedValue: String) {
            hasMarker(markername)
            assertEquals(expectedValue, markerMap[markername], "Marker value did not match")
        }

        fun markerValueContains(markername: String, expectedValue: String) {
            hasMarker(markername)
            assertEquals(expectedValue, markerMap[markername], "Marker value did not match")
        }
    }
}
