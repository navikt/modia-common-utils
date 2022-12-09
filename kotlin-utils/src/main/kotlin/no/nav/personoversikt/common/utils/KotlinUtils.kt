package no.nav.personoversikt.common.utils

object KotlinUtils {
    infix fun Int.inRange(range: Pair<Int, Int>): Boolean = this >= range.first && this < range.second
    infix fun Int.inRangeInclusive(range: Pair<Int, Int>): Boolean = this >= range.first && this <= range.second

    infix fun Int.plusminus(offset: Int): IntRange = (this - offset)..(this + offset)

    infix fun <T> ((T) -> Boolean).or(other: (T) -> Boolean): (T) -> Boolean = { this(it) or other(it) }
    infix fun <T> ((T) -> Boolean).and(other: (T) -> Boolean): (T) -> Boolean = { this(it) and other(it) }
    infix fun <T> ((T) -> Boolean).xor(other: (T) -> Boolean): (T) -> Boolean = { this(it) xor other(it) }
}
