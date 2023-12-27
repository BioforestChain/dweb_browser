package org.dweb_browser.helper

import js.collections.WeakMap

actual class WeakHashMap<K : Any, V> {
  private val wm = WeakMap<K, V>()

  actual fun containsKey(key: K): Boolean = wm.has(key)

  actual fun get(key: K): V? = wm[key]

  actual fun clear() {
  }

  actual fun put(key: K, value: V): V? = wm[key].also {
    wm[key] = value
  }

  actual fun remove(key: K): V? = wm[key].also {
    wm.delete(key)
  }
}