package org.dweb_browser.browser.nativeui

import androidx.compose.runtime.Composable
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.platform.IPureViewController

actual class NativeUiController actual constructor(pureViewController: IPureViewController) {
  @Composable
  actual fun effect(): NativeUiController {
    WARNING("Not yet implement NativeUiController.effect()")
    return this
  }
}