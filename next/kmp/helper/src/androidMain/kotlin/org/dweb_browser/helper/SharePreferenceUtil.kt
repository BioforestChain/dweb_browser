package org.dweb_browser.helper

import android.content.Context
import androidx.core.content.edit

private const val SHARED_PREFERENCES_NAME = "DwebBrowser"

/// TODO 这里统一使用 NativeMicroModule 替代 Context
public fun Context.saveString(key: String, value: String) {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  sp.edit { putString(key, value) }
}

public fun Context.getString(key: String, default: String = ""): String {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  return sp.getString(key, default) ?: ""
}

public fun Context.saveInteger(key: String, value: Int) {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  sp.edit { putInt(key, value) }
}

public fun Context.getInteger(key: String, default: Int = 0): Int {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  return sp.getInt(key, default)
}

public fun Context.saveBoolean(key: String, value: Boolean) {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  sp.edit { putBoolean(key, value) }
}

public fun Context.getBoolean(key: String, default: Boolean = false): Boolean {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  return sp.getBoolean(key, default)
}

public fun Context.saveStringSet(key: String, value: Set<String>) {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  sp.edit { putStringSet(key, value) }
}

public fun Context.getStringSet(key: String, default: Set<String>? = null): Set<String>? {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  return sp.getStringSet(key, default)
}

public fun Context.removeKey(key: String) {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  sp.edit { remove(key) }
}

public fun Context.removeKeys(keys: List<String>) {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  sp.edit {
    keys.forEach {
      remove(it)
    }
  }
}
