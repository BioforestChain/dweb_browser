package org.dweb_browser.dwebview

import platform.WebKit.WKWebsiteDataStore

internal const val IncognitoSuffix = ".incognito"

class WKWebViewProfile internal constructor(
  override val profileName: String,
  val store: WKWebsiteDataStore,
) : DWebProfile {
  val uuid get() = store.identifier
  override val isIncognito get() = !store.persistent
}
