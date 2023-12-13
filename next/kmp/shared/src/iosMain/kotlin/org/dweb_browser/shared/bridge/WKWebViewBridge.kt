package org.dweb_browser.shared.bridge

import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.engine.DWebViewEngine
import platform.UIKit.UIScreen
import platform.WebKit.WKWebViewConfiguration

class WKWebViewBridge {
  companion object {
    val shared = WKWebViewBridge()
  }

  var webBrowserNMM: MicroModule? = null

  @OptIn(ExperimentalForeignApi::class)
  fun webviewFactory(): DWebViewEngine =
    DWebViewEngine(
      UIScreen.mainScreen.bounds,
      shared.webBrowserNMM!!,
      DWebViewOptions(),
      WKWebViewConfiguration()
    )
}


