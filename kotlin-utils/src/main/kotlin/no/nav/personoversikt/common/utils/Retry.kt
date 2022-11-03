package no.nav.personoversikt.common.utils

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory.getLogger
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.pow
import kotlin.time.Duration

class Retry(private val config: Config) {

    data class Config(
        val maxRetries: Int = Int.MAX_VALUE,
        val initDelay: Duration,
        val growthFactor: Double,
        val delayLimit: Duration,
        val scheduler: Timer = Timer()
    )

    private val log = getLogger(Retry::class.java)

    suspend fun run(block: suspend () -> Unit) = run(0, block)

    private suspend fun run(attemptNumber: Int, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            val delay = (config.initDelay * config.growthFactor.pow(attemptNumber)).coerceAtMost(config.delayLimit)

            log.error("Retry failed at attempt $attemptNumber with error: ${e.message}")

            if (attemptNumber < config.maxRetries) {
                config.scheduler.schedule(delay.inWholeMilliseconds) {
                    runBlocking {
                        run(attemptNumber + 1, block)
                    }
                }
            }
        }
    }
}
