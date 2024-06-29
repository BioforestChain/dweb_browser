package org.dweb_browser.helper.platform

import org.dweb_browser.helper.toKString
import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults
import platform.Foundation.setValue
import platform.Foundation.valueForKey

actual class KeyValueStore(
  val storeName: String = "kv",
  val store: NSUserDefaults = NSUserDefaults(suiteName = storeName),
) {
  companion object {
    val instance by lazy { KeyValueStore() }
  }

  actual fun getString(storeKey: String): String? {
    return store.valueForKey(key = storeKey)
      .let { if (it is NSString) (it as NSString).toKString() else null }
  }

  actual fun setString(storeKey: String, value: String) {
    store.setValue(value = value, forKey = storeKey)
  }

  actual fun setValues(storeKey: String, values: Set<String>) =
    store.setObject(value = values.toList(), forKey = storeKey)

  actual fun getValues(storeKey: String) =
    (store.objectForKey(defaultName = storeKey) as List<*>?)?.filterIsInstance<String>()?.toSet()

  actual fun removeKeys(vararg storeKeys: String) {
    for (key in storeKeys) {
      store.removeObjectForKey(defaultName = key)
    }
  }
}

actual val keyValueStore get() = KeyValueStore.instance
