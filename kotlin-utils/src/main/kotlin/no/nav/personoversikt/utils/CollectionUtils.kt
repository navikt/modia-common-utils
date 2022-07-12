package no.nav.personoversikt.utils

object CollectionUtils {
    fun <T : Any> Collection<T>?.isNotNullOrEmpty(): Boolean = !this.isNullOrEmpty()
}
