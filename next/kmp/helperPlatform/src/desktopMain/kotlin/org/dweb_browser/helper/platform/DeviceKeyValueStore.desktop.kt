package org.dweb_browser.helper.platform

import org.dweb_browser.keychainstore.keychainDeleteItem
import org.dweb_browser.keychainstore.keychainGetItem
import org.dweb_browser.keychainstore.keychainHasItem
import org.dweb_browser.keychainstore.keychainItemKeys
import org.dweb_browser.keychainstore.keychainSetItem
import org.dweb_browser.keychainstore.keychainSupportEnumKeys


actual class DeviceKeyValueStore actual constructor(
  val storeName: String,
  /**
   * 压缩算法
   */
  val compressAlgorithm: String?,
) {
  init {
    if (compressAlgorithm != null) {
      throw Exception("no yet support compress algorithm: '$compressAlgorithm'")
    }
  }

  companion object {
    val supportEnumKeys = keychainSupportEnumKeys()
  }

  actual val supportEnumKeys: Boolean get() = DeviceKeyValueStore.supportEnumKeys
  actual fun keys(): List<String> = keychainItemKeys(storeName)
  actual fun getItem(key: String): ByteArray? = keychainGetItem(storeName, key)
  actual fun hasItem(key: String): Boolean = keychainHasItem(storeName, key)
  actual fun setItem(key: String, value: ByteArray): Boolean =
    keychainSetItem(storeName, key, value)

  actual fun deleteItem(key: String): Boolean = keychainDeleteItem(storeName, key)
}