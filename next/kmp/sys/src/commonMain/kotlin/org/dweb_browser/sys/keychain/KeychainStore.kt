package org.dweb_browser.sys.keychain

import org.dweb_browser.core.module.MicroModule

expect class KeychainStore(runtime: MicroModule.Runtime) {
  suspend fun getItem(remoteMmid: String, key: String): ByteArray?
  suspend fun setItem(remoteMmid: String, key: String, value: ByteArray): Boolean
  suspend fun hasItem(remoteMmid: String, key: String): Boolean
  suspend fun deleteItem(remoteMmid: String, key: String): Boolean
  suspend fun supportEnumKeys(): Boolean
  suspend fun keys(remoteMmid: String): List<String>
}