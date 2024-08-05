package org.dweb_browser.dwebview

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.pure.crypto.hash.sha256Sync
import platform.Foundation.NSUUID
import platform.WebKit.WKWebsiteDataStore

class WKWebViewProfile internal constructor(
  override val profileName: ProfileName,
) : DWebProfile {
  val store by lazy { profileName.dataStore }
  override val isIncognito get() = !store.persistent
}

@OptIn(ExperimentalForeignApi::class)
val ProfileName.identifier
  get() = sha256Sync(key.utf8Binary).asUByteArray().usePinned {
    NSUUID(uUIDBytes = it.addressOf(0))
  }

val ProfileName.dataStore
  get() = when (this) {
    is ProfileIncognitoNameV1 -> WKWebsiteDataStore.nonPersistentDataStore()
    is NoProfileName -> WKWebsiteDataStore.defaultDataStore()
    is ProfileNameV1, is ProfileNameV0 -> WKWebsiteDataStore.dataStoreForIdentifier(identifier)
  }