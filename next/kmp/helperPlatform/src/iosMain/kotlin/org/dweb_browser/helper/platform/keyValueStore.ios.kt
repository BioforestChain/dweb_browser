package org.dweb_browser.helper.platform

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import org.dweb_browser.helper.toKString
import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults
import platform.Foundation.setValue
import platform.Foundation.valueForKey

actual class KeyValueStore(actual val storeName: String, val store: NSUserDefaults) :
  SynchronizedObject() {
  actual constructor(storeName: String) : this(storeName, NSUserDefaults(suiteName = storeName))

  companion object {
    val instance by lazy { KeyValueStore("kv") }
  }

  actual fun getString(storeKey: String): String? {
    return store.valueForKey(key = storeKey)
      .let { if (it is NSString) (it as NSString).toKString() else null }
  }

  actual fun setString(storeKey: String, value: String) = synchronized(this) {
    store.setValue(value = value, forKey = storeKey)
  }

  actual fun setValues(storeKey: String, values: Set<String>) = synchronized(this) {
    store.setObject(value = values.toList(), forKey = storeKey)
  }

  actual fun getValues(storeKey: String) =
    (store.objectForKey(defaultName = storeKey) as List<*>?)?.filterIsInstance<String>()?.toSet()

  actual fun removeKeys(vararg storeKeys: String) = synchronized(this) {
    for (key in storeKeys) {
      store.removeObjectForKey(defaultName = key)
    }
  }

  actual inline fun getStringOrPut(storeKey: String, put: () -> String) =
    getString(storeKey) ?: put().also { setString(storeKey, it) }
}

actual val keyValueStore get() = KeyValueStore.instance
