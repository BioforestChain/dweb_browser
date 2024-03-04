package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalWindowInfo

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun rememberScreenSize(): ScreenSize {
  val windowInfo = LocalWindowInfo.current
  return ScreenSize(windowInfo.containerSize.width, windowInfo.containerSize.height)
}