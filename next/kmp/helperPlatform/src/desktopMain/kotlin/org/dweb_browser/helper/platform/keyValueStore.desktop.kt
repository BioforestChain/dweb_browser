package org.dweb_browser.helper.platform

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.printError
import java.util.prefs.Preferences

actual class KeyValueStore(actual val storeName: String, val store: Preferences) :
  SynchronizedObject() {
  actual constructor(storeName: String) : this(storeName, Preferences.userRoot().node(storeName))

  companion object {
    val instance by lazy { KeyValueStore("kv") }
  }

  actual fun getString(storeKey: String): String? {
    return store.get(storeKey, null)
  }

  actual fun setString(storeKey: String, value: String) = synchronized(this) {
    store.put(storeKey, value)
  }

  actual fun setValues(storeKey: String, values: Set<String>) = synchronized(this) {
    store.put(storeKey, Json.encodeToString(values))
  }

  actual fun getValues(storeKey: String): Set<String>? {
    val json = store.get(storeKey, null) ?: return null
    return runCatching { Json.decodeFromString<Set<String>>(json) }.getOrElse { err ->
      printError("KeyValueStore", "getValues($storeKey)", err)
      null
    }
  }

  actual fun removeKeys(vararg storeKeys: String) = synchronized(this) {
    for (storeKey in storeKeys) {
      store.remove("kv/$storeKey")
    }
  }

  actual inline fun getStringOrPut(storeKey: String, put: () -> String) =
    getString(storeKey) ?: put().also { setString(storeKey, it) }
}

actual val keyValueStore: KeyValueStore get() = KeyValueStore.instance