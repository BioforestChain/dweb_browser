package org.dweb_browser.helper.platform

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.printError
import java.util.prefs.Preferences

actual class KeyValueStore(
  val storeName: String = "kv",
  val store: Preferences = Preferences.userRoot().node(storeName),
) {

  companion object {
    val instance by lazy { KeyValueStore() }
  }

  actual fun getString(storeKey: String): String? {
    return store.get(storeKey, null)
  }

  actual fun setString(storeKey: String, value: String) {
    store.put(storeKey, value)
  }

  actual fun setValues(storeKey: String, values: Set<String>) {
    store.put(storeKey, Json.encodeToString(values))
  }

  actual fun getValues(storeKey: String): Set<String>? {
    val json = store.get(storeKey, null) ?: return null
    return runCatching { Json.decodeFromString<Set<String>>(json) }.getOrElse { err ->
      printError("KeyValueStore", "getValues($storeKey)", err)
      null
    }
  }

  actual fun removeKeys(vararg storeKeys: String) {
    for (storeKey in storeKeys) {
      store.remove("kv/$storeKey")
    }
  }
}

actual val keyValueStore: KeyValueStore get() = KeyValueStore.instance