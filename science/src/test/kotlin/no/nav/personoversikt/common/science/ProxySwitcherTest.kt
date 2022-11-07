package no.nav.personoversikt.common.science

import no.nav.personoversikt.common.test.logassert.LogAsserts.Companion.captureLogs
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.event.Level

internal class ProxySwitcherTest {
    fun interface DummyService {
        fun getData(): String
    }

    @Test
    internal fun `enabled switch should use ifEnabled service`() {
        val service: DummyService = ProxySwitcher.createSwitcher(
            switch = { true },
            ifEnabled = DummyService { "From enabled" },
            ifDisabled = DummyService { "From disabled" },
        )

        captureLogs {
            assertEquals("From enabled", service.getData())
        }
            .hasSize(1)
            .logline {
                hasLevel(Level.WARN)
                messageEquals("[ProxySwitcher] DummyService is enabled")
            }
    }

    @Test
    internal fun `disabled switch should use ifDisabled service`() {
        val service: DummyService = ProxySwitcher.createSwitcher(
            switch = { false },
            ifEnabled = DummyService { "From enabled" },
            ifDisabled = DummyService { "From disabled" },
        )

        captureLogs {
            assertEquals("From disabled", service.getData())
        }.hasSize(0)
    }

    @Test
    internal fun `exceptions from underlying service should propagate without proxy-wrapper`() {
        val service: DummyService = ProxySwitcher.createSwitcher(
            switch = { true },
            ifEnabled = DummyService { error("From enabled") },
            ifDisabled = DummyService { error("From disabled") },
        )

        val exception = assertThrows<IllegalStateException> {
            service.getData()
        }
        assertEquals("From enabled", exception.message)
    }
}
