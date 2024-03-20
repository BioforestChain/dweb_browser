package org.dweb_browser.browser.web.model.page

actual fun BrowserWebPage.requestCaptureInCompose() {
  webView.requestRefresh()
}