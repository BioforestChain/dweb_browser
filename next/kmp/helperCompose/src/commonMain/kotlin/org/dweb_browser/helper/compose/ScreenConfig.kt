package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable

data class ScreenSize(
  val screenWidth: Int,
  val screenHeight: Int,
) {
  companion object {
    val Zero = ScreenSize(0, 0)
  }
}

@Composable
expect fun rememberScreenSize(): ScreenSize