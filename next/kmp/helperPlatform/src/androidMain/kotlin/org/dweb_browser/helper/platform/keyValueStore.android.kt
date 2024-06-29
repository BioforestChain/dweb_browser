package org.dweb_browser.helper.platform

import android.content.Context
import androidx.core.content.edit
import org.dweb_browser.helper.getAppContextUnsafe

actual class KeyValueStore(
  val storeName: String = "kv",
  val context: Context = getAppContextUnsafe(),
) {
  private val sp = context.getSharedPreferences(storeName, Context.MODE_PRIVATE)

  companion object {
    val instance by lazy { KeyValueStore() }
  }

  actual fun getString(storeKey: String): String? {
    return sp.getString(storeKey, null)
  }

  actual fun setString(storeKey: String, value: String) {
    sp.edit { putString(storeKey, value) }
  }

  actual fun setValues(storeKey: String, values: Set<String>) {
    sp.edit { putStringSet(storeKey, values) }
  }

  actual fun getValues(storeKey: String) = sp.getStringSet(storeKey, null)
  actual fun removeKeys(vararg storeKeys: String) = sp.edit {
    storeKeys.forEach {
      remove(it)
    }
  }
}

actual val keyValueStore get() = KeyValueStore.instance