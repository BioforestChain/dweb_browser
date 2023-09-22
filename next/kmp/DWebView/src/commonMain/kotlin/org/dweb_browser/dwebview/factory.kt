package org.dweb_browser.dwebview

import org.dweb_browser.helper.platform.PlatformViewController
import org.dweb_browser.microservice.core.MicroModule

interface IDWebView {
  fun loadUrl(url: String)
}

expect fun createDWebView(platformViewController: PlatformViewController, remoteMM: MicroModule): IDWebView
