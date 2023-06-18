package info.bagen.dwebbrowser.microService.browser.nativeui.helper

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.core.view.*
import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

@JsonAdapter(BarStyle::class)
enum class BarStyle(val style: String) : JsonDeserializer<BarStyle>, JsonSerializer<BarStyle> {
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
        fun from(style: String): BarStyle {
            return values().find { it.style == style } ?: Default
        }
    }

    override fun deserialize(
        json: JsonElement, typeOfT: Type, context: JsonDeserializationContext
    ): BarStyle = from(json.asString)

    override fun serialize(
      src: BarStyle, typeOfSrc: Type, context: JsonSerializationContext
    ): JsonElement = JsonPrimitive(src.style)

}
