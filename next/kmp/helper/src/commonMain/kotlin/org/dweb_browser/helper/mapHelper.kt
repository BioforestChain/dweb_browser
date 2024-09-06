@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package org.dweb_browser.helper


public fun <K, V> Map<K, V>.getOrDefault(key: K, default: V): V = get(key) ?: default
public fun <K, V> MutableMap<K, V>.remove(key: K, value: V): Boolean =
  (get(key) == value).trueAlso {
    remove(key)
  }
