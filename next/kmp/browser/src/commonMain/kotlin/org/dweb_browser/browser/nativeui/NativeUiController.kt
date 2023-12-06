package org.dweb_browser.browser.nativeui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.noLocalProvidedFor

val LocalPureViewController = compositionLocalOf<IPureViewController> {
  noLocalProvidedFor("LocalNativeUiController")
}

expect class NativeUiController(pureViewController: IPureViewController) {
  @Composable
  fun effect(): NativeUiController
}