package org.dweb_browser.microservice.sys.http

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.StringEnumSerializer
import java.lang.reflect.Type

object MatchModeSerializer :
  StringEnumSerializer<MatchMode>("MatchMode", MatchMode.ALL_VALUES, { mode })

@Serializable(MatchModeSerializer::class)
@JsonAdapter(MatchMode::class)
enum class MatchMode(val mode: String) : JsonDeserializer<MatchMode>, JsonSerializer<MatchMode> {
  FULL("full"), PREFIX("prefix"), ;

  companion object {
    val ALL_VALUES = values().associateBy { it.mode }
  }

  override fun deserialize(
    json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?
  ) = json.asString.let { mode -> values().first { it.mode == mode } }

  override fun serialize(
    src: MatchMode, typeOfSrc: Type?, context: JsonSerializationContext?
  ) = JsonPrimitive(src.mode)
}