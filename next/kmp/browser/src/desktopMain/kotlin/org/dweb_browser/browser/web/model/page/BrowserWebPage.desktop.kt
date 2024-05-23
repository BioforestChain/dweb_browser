package org.dweb_browser.browser.web.model.page

import org.dweb_browser.dwebview.asDesktop

actual fun BrowserWebPage.requestCaptureInCompose() {
  kotlin.runCatching {
    thumbnail = webView.asDesktop().viewEngine.getCaptureImage()
  }
}