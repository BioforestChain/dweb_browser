package org.dweb_browser.sys.keychain

import org.dweb_browser.helper.platform.DeviceKeyValueStore
import org.dweb_browser.helper.utf8Binary

actual class KeyChainStore {
  actual fun getItem(scope: String, key: String): ByteArray? {
    val store = DeviceKeyValueStore(scope)
    return store.getRawItem(key.utf8Binary)
  }

  actual fun setItem(
    scope: String,
    key: String,
    value: ByteArray,
  ): Boolean {
    val store = DeviceKeyValueStore(scope)
    return runCatching { store.setRawItem(key.utf8Binary, value);true }.getOrDefault(false)
  }

  actual fun hasItem(scope: String, key: String): Boolean {
    val store = DeviceKeyValueStore(scope)
    return store.hasKey(key)
  }

  actual fun deleteItem(scope: String, key: String): Boolean {
    val store = DeviceKeyValueStore(scope)
    return store.removeKey(key)
  }

  actual fun supportEnumKeys(): Boolean {
    return true
  }

  actual fun keys(scope: String): List<String> {
    val store = DeviceKeyValueStore(scope)
    return store.getKeys()
  }
}