package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.dweb_browser.helper.platform.PlatformViewController

@Composable
actual fun _rememberPlatformViewController() = LocalContext.current.let { context ->
  remember(context) {
    PlatformViewController(context)
  }
}