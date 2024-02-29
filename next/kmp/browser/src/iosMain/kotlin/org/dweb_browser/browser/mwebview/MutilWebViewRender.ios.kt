package org.dweb_browser.browser.mwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import org.dweb_browser.dwebview.asIosUIView
import org.dweb_browser.sys.window.render.WindowFrameStyleEffect
import platform.UIKit.UIView

@Composable
actual fun MultiWebViewController.AfterViewItemRender(viewItem: MultiWebViewController.MultiViewItem) {
  var view: UIView? = remember { viewItem.webView.asIosUIView() }
  LaunchedEffect(viewItem) {
    viewItem.webView.onDestroy {
      view = null
    }
  }
  view?.WindowFrameStyleEffect()
}