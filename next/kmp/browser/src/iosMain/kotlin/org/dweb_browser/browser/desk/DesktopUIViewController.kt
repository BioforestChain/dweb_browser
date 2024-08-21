package org.dweb_browser.browser.desk

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.pure.image.compose.rememberOffscreenWebCanvas
import org.dweb_browser.sys.window.render.LocalWindowsImeVisible

@OptIn(ExperimentalForeignApi::class)
class DesktopUIViewController(val vc: PureViewController) {
  init {
    DeskViewController(vc)

    vc.addContent {
      /// 对 imeVisible 的绑定支持
      val imeInsets = WindowInsets.ime
      val imeVisibleState = LocalWindowsImeVisible.current
      val density = LocalDensity.current
      LaunchedEffect(imeInsets, density) {
        imeVisibleState.value = imeInsets.getBottom(density) != 0
      }
    }
    if (false) {
      @OptIn(InternalComposeApi::class)
      vc.addContent {
        val offscreenWebCanvas = rememberOffscreenWebCanvas()
        LaunchedEffect(offscreenWebCanvas) {
          vc.getUiViewController().view.addSubview(offscreenWebCanvas.webview)
        }
      }
    }
  }
}