package no.nav.personoversikt.common.utils

object CollectionUtils {
    fun <T : Any> Collection<T>?.isNotNullOrEmpty(): Boolean = !this.isNullOrEmpty()
}
