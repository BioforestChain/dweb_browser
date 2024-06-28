package org.dweb_browser.helper.platform

import android.content.Context
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.helper.getStringSet
import org.dweb_browser.helper.removeKeys
import org.dweb_browser.helper.saveStringSet

class KeyValueStore private constructor(val context: Context = getAppContextUnsafe()) {
  companion object {
    val instance by lazy { KeyValueStore() }
  }

  fun setValues(storeKey: String, values: Set<String>) =
    context.saveStringSet(storeKey, values)

  fun getValues(storeKey: String) = context.getStringSet(storeKey)
  fun removeKeys(vararg storeKeys: String) =
    context.removeKeys(storeKeys.toList())

}

val keyValueStore get() = KeyValueStore.instance