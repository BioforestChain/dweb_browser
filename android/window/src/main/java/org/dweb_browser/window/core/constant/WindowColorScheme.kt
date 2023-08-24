package org.dweb_browser.window.core.constant

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

@JsonAdapter(WindowColorScheme::class)
enum class WindowColorScheme(val scheme: String) : JsonSerializer<WindowColorScheme>,
  JsonDeserializer<WindowColorScheme>, Comparator<WindowColorScheme> {
  Normal("normal"),
  Light("light"),
  Dark("dark"),
  ;

  companion object {
    val ALL_VALUES = values().toList()
    fun from(themeName: String) = ALL_VALUES.firstOrNull { it.scheme == themeName } ?: Normal
  }

  fun next() = ALL_VALUES[(ALL_VALUES.indexOf(this) + 1) % ALL_VALUES.size]

  override fun compare(o1: WindowColorScheme, o2: WindowColorScheme): Int {
    return o1.scheme.compareTo(o2.scheme)
  }

  override fun serialize(
    src: WindowColorScheme,
    typeOfSrc: Type,
    context: JsonSerializationContext?
  ) = JsonPrimitive(src.scheme)

  override fun deserialize(
    json: JsonElement,
    typeOfT: Type?,
    context: JsonDeserializationContext?
  ) = from(json.asString)

}