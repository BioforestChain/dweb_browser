package org.dweb_browser.helper.compose

fun java.awt.Color.toComposeColor() =
  androidx.compose.ui.graphics.Color(red = red, green = green, blue = blue, alpha = alpha)

fun androidx.compose.ui.graphics.Color.toAwtColor() = java.awt.Color(red, green, blue, alpha)