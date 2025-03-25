//package org.dweb_browser.sys.keychain
//
//import org.dweb_browser.keychainstore.keychainDeleteItem
//import org.dweb_browser.keychainstore.keychainGetItem
//import org.dweb_browser.keychainstore.keychainHasItem
//import org.dweb_browser.keychainstore.keychainItemKeys
//import org.dweb_browser.keychainstore.keychainSetItem
//import org.dweb_browser.keychainstore.keychainSupportEnumKeys
//
//actual class KeyChainStore {
//  actual fun getItem(scope: String, key: String): ByteArray? {
//    return keychainGetItem(scope, key)
//  }
//
//  actual fun setItem(
//    scope: String,
//    key: String,
//    value: ByteArray,
//  ): Boolean {
//    return keychainSetItem(scope, key, value)
//  }
//
//
//  actual fun hasItem(scope: String, key: String): Boolean {
//    return keychainHasItem(scope, key)
//  }
//
//  actual fun deleteItem(scope: String, key: String): Boolean {
//    return keychainDeleteItem(scope, key)
//  }
//
//  actual fun supportEnumKeys(): Boolean {
//    return keychainSupportEnumKeys()
//  }
//
//  actual fun keys(scope: String): List<String> {
//    return keychainItemKeys(scope)
//  }
//}