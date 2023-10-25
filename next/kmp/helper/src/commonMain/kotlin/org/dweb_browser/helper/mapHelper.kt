package org.dweb_browser.helper


inline fun <K, V> Map<K, V>.getOrDefault(key: K, default: V) = get(key) ?: default
inline fun <K, V> MutableMap<K, V>.remove(key: K, value: V) = (get(key) == value).trueAlso {
  remove(key)
}
