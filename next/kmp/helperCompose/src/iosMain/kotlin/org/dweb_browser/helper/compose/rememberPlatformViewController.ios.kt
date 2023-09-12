package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController
import org.dweb_browser.helper.platform.PlatformViewController
import platform.UIKit.UIScreen


@Composable
actual fun _rememberPlatformViewController(): PlatformViewController {
  val controller = LocalUIViewController.current
  return remember(controller) {
    PlatformViewController(controller, UIScreen.mainScreen)
  }
}
