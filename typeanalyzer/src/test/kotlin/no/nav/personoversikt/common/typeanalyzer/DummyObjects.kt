package no.nav.personoversikt.common.typeanalyzer

import java.util.*

data class RootObject(
    val id: String? = null,
    val nullValue: String? = null,
    val active: Boolean? = null,
    val count: Int? = null,
    val countLong: Long? = null,
    val fraction: Double? = null,
    val fractionFloat: Float? = null,
    val emptyArray: Array<Stuff?>? = null,
    val listOfStuff: List<Stuff?>? = null,
)

data class Stuff(
    val id: UUID? = null,
    val meta: Map<String, String?>? = null
)

val valueObject = RootObject(
    id = "id",
    nullValue = null,
    active = true,
    count = 123,
    countLong = 123,
    fraction = 0.3,
    fractionFloat = 0.3f,
    emptyArray = emptyArray(),
    listOfStuff = listOf(
        Stuff(
            id = UUID.randomUUID(),
            meta = mapOf("key" to "value", "key2" to "other")
        ),
    )
)
