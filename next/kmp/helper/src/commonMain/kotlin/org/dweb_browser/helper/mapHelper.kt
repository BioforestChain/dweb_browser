package org.dweb_browser.helper


fun <K, V> Map<K, V>.getOrDefault(key: K, default: V) = get(key) ?: default
fun <K, V> MutableMap<K, V>.remove(key: K, value: V) = (get(key) == value).trueAlso {
  remove(key)
}
