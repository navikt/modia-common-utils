package no.nav.personoversikt.test.snapshot.format

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator
import no.nav.personoversikt.test.snapshot.SnapshotRunner

private class JsonFormat(private val mapper: ObjectMapper) : SnapshotRunner.Fileformat {
    override val fileExtension: String = "json"

    override fun write(value: Any): String = mapper
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(value)

    override fun read(value: String): Any = mapper.readTree(value)

    companion object {
        val plain: JsonMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .build()

        val typed: JsonMapper = plain
            .rebuild()
            .activateDefaultTyping(DefaultBaseTypeLimitingValidator())
            .build()
    }
}

object JsonSnapshotFormat : SnapshotRunner.Fileformat by JsonFormat(JsonFormat.typed) {
    val typed: SnapshotRunner.Fileformat = JsonFormat(JsonFormat.typed)
    val plain: SnapshotRunner.Fileformat = JsonFormat(JsonFormat.plain)
}
