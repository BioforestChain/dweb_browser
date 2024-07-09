//package org.dweb_browser.sys.keychain
//
//import keychainstore.keychainDeleteItem
//import keychainstore.keychainGetItem
//import keychainstore.keychainHasItem
//import keychainstore.keychainItemKeys
//import keychainstore.keychainSetItem
//import keychainstore.keychainSupportEnumKeys
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