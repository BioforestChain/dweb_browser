package org.dweb_browser.helper.compose

import androidx.compose.ui.graphics.Color
import platform.UIKit.UIColor


fun UIColor.Companion.fromColorInt(color: Int) = UIColor(
  red = ((color ushr 16) and 0xFF) / 255.0,
  green = ((color ushr 8) and 0xFF) / 255.0,
  blue = ((color) and 0xFF) / 255.0,
  alpha = 1.0
)

fun UIColor.Companion.fromColor(color: Color) = UIColor(
  red = color.red.toDouble(),
  green = color.green.toDouble(),
  blue = color.blue.toDouble(),
  alpha = color.alpha.toDouble(),
)

val UIColor.Companion.transparentColor by lazy { UIColor.clearColor }

fun Color.toUIColor() = UIColor.fromColor(this)
fun UIColor.toComposeColor() = with(CIColor) {
  Color(
    red = red.toFloat(),
    green = green.toFloat(),
    blue = blue.toFloat(),
    alpha = alpha.toFloat()
  )
}