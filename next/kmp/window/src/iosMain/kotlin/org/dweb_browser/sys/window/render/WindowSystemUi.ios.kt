package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.ios.SecureViewController
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowsManagerState.Companion.watchedState
import platform.UIKit.UIViewController

@Composable
actual fun <T : WindowController> WindowsManager<T>.EffectKeyboard() {
//  IOS 目前好像不需要对键盘做额外的处理，它能自己很好地全局定位到input的位置
}

@Composable
actual fun <T : WindowController> WindowsManager<T>.EffectNavigationBar() {
  WARNING("Not yet implemented EffectNavigationBar")

}

@Composable
actual fun NativeBackHandler(
  enabled: Boolean,
  onBack: () -> Unit
) {
  WARNING("Not yet implemented NativeBackHandler")
}


@OptIn(ExperimentalForeignApi::class)
val secureVcWM = WeakHashMap<UIViewController, SecureViewController>()

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun <T : WindowController> WindowsManager<T>.EffectSafeModel() {
  val safeMode by watchedState { safeMode }
  val vc = LocalUIViewController.current
  val secureViewController = remember(vc) {
    secureVcWM.getOrPut(vc) {
      SecureViewController(vc = vc, onNewView = null)
    }
  }

  if (safeMode) {
    DisposableEffect(secureViewController) {
      secureViewController.setSafeMode(safe = true)
      onDispose {
        secureViewController.setSafeMode(safe = false)
      }
    }
  }
}