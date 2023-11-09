package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable

@Composable
actual fun rememberScreenSize(): ScreenSize {
  return ScreenSize.Zero
}