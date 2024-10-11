package no.nav.personoversikt.common.science

import kotlin.random.Random

fun interface Rate {
    fun evaluate(): Boolean

    class FixedValue(
        private val rate: Double,
        private val random: Random = Random.Default,
    ) : Rate {
        override fun evaluate(): Boolean = random.nextDouble() < rate
    }
}
