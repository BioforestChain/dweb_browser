package org.dweb_browser.helper.platform

import platform.Foundation.NSUserDefaults

class KeyValueStore private constructor(val store: NSUserDefaults = NSUserDefaults.standardUserDefaults) {
  companion object {
    val instance by lazy { KeyValueStore() }
  }

  fun setValues(storeKey: String, values: Set<String>) =
    store.setObject(value = values.toList(), forKey = storeKey)

  fun getValues(storeKey: String) =
    (store.objectForKey(defaultName = storeKey) as List<*>?)?.filterIsInstance<String>()?.toSet()

  fun removeKeys(vararg storeKeys: String) {
    for (key in storeKeys) {
      store.removeObjectForKey(defaultName = key)
    }
  }
}

val keyValueStore get() = KeyValueStore.instance
