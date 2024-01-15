package org.dweb_browser.helper

import js.collections.WeakMap

open class JsWeakHashMap<K : Any, V> {
  private val wm = WeakMap<K, V>()

  fun containsKey(key: K): Boolean = wm.has(key)

  fun get(key: K): V? = wm[key]

  fun clear() {
  }

  fun put(key: K, value: V): V? = wm[key].also {
    wm[key] = value
  }

  fun remove(key: K): V? = wm[key].also {
    wm.delete(key)
  }
}