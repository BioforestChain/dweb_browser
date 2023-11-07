package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
actual fun rememberScreenSize(): ScreenSize {
  val config = LocalConfiguration.current
  return ScreenSize(config.screenWidthDp, config.screenHeightDp)
}