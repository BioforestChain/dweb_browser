package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController

@Composable
actual fun NativeBackHandler(enabled: Boolean, onBack: () -> Unit) {
  DisposableEffect(Unit) {
    val off = nativeViewController.onGoBack {
      if (enabled) {
        onBack()
      }
    }
    onDispose {
      off()
    }
  }
}
