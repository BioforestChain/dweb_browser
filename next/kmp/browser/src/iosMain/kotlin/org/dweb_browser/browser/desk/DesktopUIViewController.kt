package org.dweb_browser.browser.desk

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.pure.image.compose.rememberOffscreenWebCanvas
import org.dweb_browser.sys.window.render.LocalWindowsImeVisible

class DesktopUIViewController(val vc: PureViewController) {
  init {
    DesktopViewControllerCore(vc)

    vc.addContent {
      /// 对 imeVisible 的绑定支持
      val imeInsets = WindowInsets.ime
      val imeVisibleState = LocalWindowsImeVisible.current
      val density = LocalDensity.current
      LaunchedEffect(imeInsets, density) {
        imeVisibleState.value = imeInsets.getBottom(density) != 0
      }
    }
    @OptIn(InternalComposeApi::class)
    vc.addContent {
      rememberOffscreenWebCanvas()
    }
  }
}