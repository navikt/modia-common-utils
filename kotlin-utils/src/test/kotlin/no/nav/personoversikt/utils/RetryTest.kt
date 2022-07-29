package no.nav.personoversikt.utils

import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

internal class RetryTest {

    @Test
    fun `skal fungere uten å kjøre retry`() = runBlocking {
        val scheduleMock: Timer = mockk()
        val retry = retryFactory(scheduleMock)

        retry.run { "Noe som fungerer" }

        verify {
            scheduleMock wasNot Called
        }
    }

    @Test
    fun `skal kjøre retry`() = runBlocking {
        val scheduleMock: Timer = mockk()
        val retry = retryFactory(scheduleMock)
        every { scheduleMock.schedule(any(), any<Long>()) } returns Unit

        retry.run { throw Exception() }

        verify {
            scheduleMock.schedule(any(), 100)
        }
    }

    @Test
    fun `skal kjøre retry 5 ganger`() = runBlocking {
        val schedulerSpy = spyk<Timer>()
        val retry = retryFactory(schedulerSpy)

        retry.run { throw Exception() }

        delay(2000)
        verifySequence {
            schedulerSpy.schedule(any(), 100)
            schedulerSpy.schedule(any(), 200)
            schedulerSpy.schedule(any(), 400)
            schedulerSpy.schedule(any(), 500)
            schedulerSpy.schedule(any(), 500)
        }
    }

    private fun retryFactory(schedulerSpy: Timer) = Retry(
        Retry.Config(
            maxRetries = 5,
            initDelay = 100.milliseconds,
            growthFactor = 2.0,
            delayLimit = 500.milliseconds,
            scheduler = schedulerSpy
        )
    )
}
