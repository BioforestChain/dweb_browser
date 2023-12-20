package org.dweb_browser.helper.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlinx.serialization.Serializable

@Serializable
data class ColorJson(val red: Int, val blue: Int, val green: Int, val alpha: Int) {
  fun toColor() = Color(red = red, blue = blue, green = green, alpha = alpha)
}

fun Color.toJsonAble(): ColorJson = convert(ColorSpaces.Srgb).let {
  ColorJson(
    (it.red * 255).toInt(),
    (it.blue * 255).toInt(),
    (it.green * 255).toInt(),
    (it.alpha * 255).toInt()
  )
}

fun Color.toCssRgba(): String = convert(ColorSpaces.Srgb).let {
  "rgb(${it.red * 255} ${it.green * 255} ${it.blue * 255} ${if (it.alpha >= 1f) "" else "/ ${it.alpha}"})"
}

fun Float.to2Hex() = (this * 255).toInt().toString(16).padStart(2, '0')
fun Color.toHex(alpha: Boolean = true): String = convert(ColorSpaces.Srgb).let {
  "#(${it.red.to2Hex()}${it.green.to2Hex()}${it.blue.to2Hex()}${if (alpha && it.alpha < 1f) it.alpha.to2Hex() else ""})"
}

@OptIn(ExperimentalStdlibApi::class)
fun String.asColorHex(start: Int = 0, len: Int = 2): Int {
  var hex = this.slice(start..<(start + len))
  if (hex.length == 1) {
    hex += hex
  }
  return hex.toInt(16)
}

fun Color.Companion.hex(hex: String) = try {
  if (hex[0] == '#') when (hex.length) {
    // #RGB
    4 -> Color(hex.asColorHex(1, 1), hex.asColorHex(2, 1), hex.asColorHex(3, 1))
    // #RGBA
    5 -> Color(
      hex.asColorHex(1, 1),
      hex.asColorHex(2, 1),
      hex.asColorHex(3, 1),
      hex.asColorHex(4, 1)
    )
    // #RRGGBB
    7 -> Color(hex.asColorHex(1), hex.asColorHex(3), hex.asColorHex(5))
    // #RRGGBBAA
    9 -> Color(hex.asColorHex(1), hex.asColorHex(3), hex.asColorHex(5), hex.asColorHex(7))
    else -> null
  } else null
} catch (e: Throwable) {
  null
}