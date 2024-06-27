package org.dweb_browser.dwebview

import platform.WebKit.WKWebsiteDataStore

class WKWebViewProfile internal constructor(
  override val profileName: String,
  val store: WKWebsiteDataStore,
) : DWebProfile {
}
