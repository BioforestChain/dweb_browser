package org.dweb_browser.helper

expect class WeakHashMap<K : Any, V>() {
  fun containsKey(key: K): Boolean
  fun get(key: K): V?
  fun clear()
  fun put(key: K, value: V): V?
  fun remove(key: K): V?
}

inline fun <K : Any, V> WeakHashMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
  val value = get(key)
  return if (value == null) {
    val answer = defaultValue()
    put(key, answer)
    answer
  } else {
    value
  }
}