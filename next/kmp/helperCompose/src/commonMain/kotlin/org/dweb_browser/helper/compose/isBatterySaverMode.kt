package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.Dp

@Composable
expect fun isBatterySaverMode(): Boolean

fun Modifier.saveBlur(blur: Dp) = Modifier.composed {
  when {
    isBatterySaverMode() -> this
    else -> blur(blur)
  }
}