package org.dweb_browser.helper.platform

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

expect class KeyValueStore(storeName: String) {
  val storeName: String
  fun getString(storeKey: String): String?
  fun setString(storeKey: String, value: String)
  fun setValues(storeKey: String, values: Set<String>): Unit
  fun getValues(storeKey: String): Set<String>?
  fun removeKeys(vararg storeKeys: String): Unit

  inline fun getStringOrPut(storeKey: String, put: () -> String): String
}

inline fun <reified T : Any> KeyValueStore.getJsonOrPut(storeKey: String, put: () -> T): T {
  var res: T? = null
  val resJson = getStringOrPut(storeKey) {
    put().let {
      res = it
      Json.encodeToString(it)
    }
  }
  return res ?: runCatching { Json.decodeFromString<T>(resJson) }.getOrElse {
    put().also {
      setString(storeKey, Json.encodeToString(it))
    }
  }
}

expect val keyValueStore: KeyValueStore