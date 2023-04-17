package info.bagen.dwebbrowser.microService.sys.http

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

@JsonAdapter(MatchMode::class)
enum class MatchMode(val mode: String) : JsonDeserializer<MatchMode>, JsonSerializer<MatchMode> {
    FULL("full"),
    PREFIX("prefix"),
    ;

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ) = json.asString.let { mode -> values().first { it.mode == mode } }

    override fun serialize(
      src: MatchMode,
      typeOfSrc: Type?,
      context: JsonSerializationContext?
    ) = JsonPrimitive(src.mode)
}