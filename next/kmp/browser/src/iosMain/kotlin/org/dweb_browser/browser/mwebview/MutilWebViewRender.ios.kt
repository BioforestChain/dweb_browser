package org.dweb_browser.browser.mwebview

import androidx.compose.runtime.Composable
import org.dweb_browser.dwebview.asIosWebView
import org.dweb_browser.sys.window.render.WindowFrameStyleEffect

@Composable
actual fun MultiWebViewController.AfterViewItemRender(viewItem: MultiWebViewController.MultiViewItem) {
  viewItem.webView.asIosWebView().WindowFrameStyleEffect()
}