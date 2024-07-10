package org.dweb_browser.sys.keychain

import keychainstore.keychainDeleteItem
import keychainstore.keychainGetItem
import keychainstore.keychainHasItem
import keychainstore.keychainItemKeys
import keychainstore.keychainSetItem
import keychainstore.keychainSupportEnumKeys
import org.dweb_browser.core.module.MicroModule

actual class KeychainStore actual constructor(val runtime: MicroModule.Runtime) {
  actual suspend fun getItem(remoteMmid: String, key: String): ByteArray? {
    return keychainGetItem(remoteMmid, key)
  }

  actual suspend fun setItem(
    remoteMmid: String,
    key: String,
    value: ByteArray,
  ): Boolean {
    return keychainSetItem(remoteMmid, key, value)
  }


  actual suspend fun hasItem(remoteMmid: String, key: String): Boolean {
    return keychainHasItem(remoteMmid, key)
  }

  actual suspend fun deleteItem(remoteMmid: String, key: String): Boolean {
    return keychainDeleteItem(remoteMmid, key)
  }

  actual suspend fun supportEnumKeys(): Boolean {
    return keychainSupportEnumKeys()
  }

  actual suspend fun keys(remoteMmid: String): List<String> {
    return keychainItemKeys(remoteMmid)
  }
}