package org.dweb_browser.sys.window.core.helper

import androidx.compose.ui.graphics.Color
import org.dweb_browser.helper.compose.hex

fun String.asWindowStateColorOr(autoColor: () -> Color) =
  Color.hex(this) ?: autoColor()

fun String.asWindowStateColorOr(lightColor: Color, darkColor: Color, isDark: Boolean) =
  asWindowStateColorOr { if (isDark) darkColor else lightColor }

fun String.asWindowStateColorOr(lightColor: () -> Color, darkColor: () -> Color, isDark: Boolean) =
  asWindowStateColorOr { if (isDark) darkColor() else lightColor() }

fun String.asWindowStateColorOr(autoColor: Color) = asWindowStateColorOr { autoColor }