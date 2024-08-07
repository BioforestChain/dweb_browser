package org.dweb_browser.browser.web.model.page

import org.dweb_browser.dwebview.asIosWebView

actual fun BrowserWebPage.requestCaptureInCompose() {
  thumbnail = webView.asIosWebView().getCaptureImage()
}