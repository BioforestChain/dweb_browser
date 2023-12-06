package org.dweb_browser.browser.nativeui

import androidx.compose.runtime.Composable
import org.dweb_browser.helper.platform.IPureViewController

expect class NativeUiController(pureViewController: IPureViewController) {
  @Composable
  fun effect(): NativeUiController
}