package org.dweb_browser.browser.web.util

import android.content.Context
import androidx.core.content.edit

private const val SHARED_PREFERENCES_NAME = "dwebBrowser"
const val KEY_ENABLE_AGREEMENT = "enable.agreement" // 判断是否第一次运行程序
const val KEY_FIRST_LAUNCH = "app.first.launch" // 判断是否第一次运行程序
const val KEY_MEDIA_IS_LOADED = "media.is.loaded" // 判断media数据是否已经加载过了
const val KEY_NO_TRACE = "browser.no.trace" // 无痕浏览
const val KEY_LAST_SEARCH_KEY = "browser.last.keyword" // 最后一次使用引擎搜索的关键字

/// TODO 这里统一使用 NativeMicroModule 替代 Context
fun Context.saveString(key: String, value: String) {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  sp.edit { putString(key, value) }
}

fun Context.getString(key: String, default: String = ""): String {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  return sp.getString(key, default) ?: ""
}

fun Context.saveInteger(key: String, value: Int) {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  sp.edit { putInt(key, value) }
}

fun Context.getInteger(key: String, default: Int = 0): Int {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  return sp.getInt(key, default)
}

fun Context.saveBoolean(key: String, value: Boolean) {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  sp.edit { putBoolean(key, value) }
}

fun Context.getBoolean(key: String, default: Boolean = false): Boolean {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  return sp.getBoolean(key, default)
}

fun Context.saveStringSet(key: String, value: Set<String>) {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  sp.edit { putStringSet(key, value) }
}

fun Context.saveList(key: String, value: List<String>) {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  sp.edit { putStringSet(key, value.toSet()) }
}

fun Context.getList(key: String): MutableList<String>? {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  return getStringSet(key)?.toMutableList()
}


fun Context.getStringSet(key: String, default: Set<String>? = null): Set<String>? {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  return sp.getStringSet(key, default)
}

fun Context.remove(key: String? = null, keys: ArrayList<String>? = null) {
  val sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  key?.let {
    sp.edit { remove(it) }
  }
  keys?.forEach {
    sp.edit { remove(it) }
  }
}
