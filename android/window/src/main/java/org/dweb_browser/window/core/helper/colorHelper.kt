package org.dweb_browser.window.core.helper

import androidx.compose.ui.graphics.Color
import org.dweb_browser.helper.android.hex

fun String.asWindowStateColor(autoColor: () -> Color) =
  Color.hex(this) ?: autoColor()

fun String.asWindowStateColor(lightColor: Color, darkColor: Color, isDark: Boolean) =
  asWindowStateColor { if (isDark) darkColor else lightColor }

fun String.asWindowStateColor(autoColor: Color) = asWindowStateColor { autoColor }