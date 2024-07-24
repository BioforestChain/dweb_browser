package org.dweb_browser.helper.platform

import android.content.Context
import androidx.core.content.edit
import kotlinx.atomicfu.locks.SynchronizedObject
import org.dweb_browser.helper.getAppContextUnsafe

actual class KeyValueStore(actual val storeName: String, val context: Context) :
  SynchronizedObject() {
  actual constructor(storeName: String) : this(storeName, getAppContextUnsafe())

  private val sp = context.getSharedPreferences(storeName, Context.MODE_PRIVATE)

  companion object {
    val instance by lazy { KeyValueStore("kv") }
  }

  actual fun getString(storeKey: String): String? {
    return sp.getString(storeKey, null)
  }

  actual fun setString(storeKey: String, value: String) = synchronized(this) {
    sp.edit { putString(storeKey, value) }
  }

  actual fun setValues(storeKey: String, values: Set<String>) = synchronized(this) {
    sp.edit { putStringSet(storeKey, values) }
  }

  actual fun getValues(storeKey: String) = sp.getStringSet(storeKey, null)
  actual fun removeKeys(vararg storeKeys: String) = synchronized(this) {
    sp.edit {
      storeKeys.forEach {
        remove(it)
      }
    }
  }

  actual inline fun getStringOrPut(storeKey: String, put: () -> String) =
    getString(storeKey) ?: put().also { setString(storeKey, it) }
}

actual val keyValueStore get() = KeyValueStore.instance