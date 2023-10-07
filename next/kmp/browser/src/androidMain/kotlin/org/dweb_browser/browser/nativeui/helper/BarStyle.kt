package org.dweb_browser.browser.nativeui.helper

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

@JsonAdapter(org.dweb_browser.browser.nativeui.helper.BarStyle::class)
enum class BarStyle(val style: String) : JsonDeserializer<org.dweb_browser.browser.nativeui.helper.BarStyle>, JsonSerializer<org.dweb_browser.browser.nativeui.helper.BarStyle> {
  /**
   * Light text for dark backgrounds.
   */
  Dark("DARK"),

  /**
   * Dark text for light backgrounds.
   */
  Light("LIGHT"),

  /**
   * The style is based on the device appearance.
   * If the device is using Dark mode, the bar text will be light.
   * If the device is using Light mode, the bar text will be dark.
   * On Android the default will be the one the app was launched with.
   */
  Default("DEFAULT"), ;

  companion object {
    fun from(style: String): org.dweb_browser.browser.nativeui.helper.BarStyle {
      return values().find { it.style == style } ?: org.dweb_browser.browser.nativeui.helper.BarStyle.Default
    }
  }

  override fun deserialize(
    json: JsonElement, typeOfT: Type, context: JsonDeserializationContext
  ): org.dweb_browser.browser.nativeui.helper.BarStyle =
    org.dweb_browser.browser.nativeui.helper.BarStyle.Companion.from(json.asString)

  override fun serialize(
    src: org.dweb_browser.browser.nativeui.helper.BarStyle, typeOfSrc: Type, context: JsonSerializationContext
  ): JsonElement = JsonPrimitive(src.style)

}
