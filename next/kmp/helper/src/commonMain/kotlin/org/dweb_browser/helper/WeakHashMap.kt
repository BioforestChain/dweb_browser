package org.dweb_browser.helper

public expect class WeakHashMap<K : Any, V>() {
  public fun containsKey(key: K): Boolean
  public fun get(key: K): V?
  public fun clear()
  public fun put(key: K, value: V): V?
  public fun remove(key: K): V?
}

public operator fun <K : Any, V> WeakHashMap<K, V>.get(key: K): V? = get(key)

public inline fun <K : Any, V> WeakHashMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
  val value = get(key)
  return if (value == null) {
    val answer = defaultValue()
    put(key, answer)
    answer
  } else {
    value
  }
}