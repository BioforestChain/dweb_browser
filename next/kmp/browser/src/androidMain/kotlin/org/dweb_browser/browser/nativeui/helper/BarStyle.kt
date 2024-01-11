package org.dweb_browser.browser.nativeui.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.StringEnumSerializer

object BarStyle_Serializer :
  StringEnumSerializer<BarStyle>("BarStyle", BarStyle.ALL_VALUES, { style })

@Serializable(with = BarStyle_Serializer::class)
enum class BarStyle(val style: String) {
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
    val ALL_VALUES = entries.associateBy { it.style }
  }
}
