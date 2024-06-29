package org.dweb_browser.helper.platform

expect class KeyValueStore {
  fun getString(storeKey: String): String?
  fun setString(storeKey: String, value: String)
  fun setValues(storeKey: String, values: Set<String>): Unit
  fun getValues(storeKey: String): Set<String>?
  fun removeKeys(vararg storeKeys: String): Unit
}

expect val keyValueStore: KeyValueStore