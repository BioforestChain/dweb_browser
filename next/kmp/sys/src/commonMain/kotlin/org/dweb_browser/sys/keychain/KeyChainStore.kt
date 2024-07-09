package org.dweb_browser.sys.keychain

expect class KeyChainStore() {
  fun getItem(scope: String, key: String): ByteArray?
  fun setItem(scope: String, key: String, value: ByteArray): Boolean
  fun hasItem(scope: String, key: String): Boolean
  fun deleteItem(scope: String, key: String): Boolean
  fun supportEnumKeys(): Boolean
  fun keys(scope: String): List<String>
}