package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import org.dweb_browser.helper.WARNING

@Composable
actual fun rememberScreenSize(): ScreenSize {
  WARNING("Not yet implemented rememberScreenSize")
  return ScreenSize.Zero
}