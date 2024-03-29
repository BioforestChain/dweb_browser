package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import platform.Foundation.NSMapTable


actual class WeakHashMap<K : Any, V> {
  private val lock = SynchronizedObject()
  private val _weakMap = NSMapTable.weakToStrongObjectsMapTable()

  actual fun containsKey(key: K): Boolean {
    return _weakMap.objectForKey(key) != null
  }

  actual fun get(key: K): V? = synchronized(lock) {
    @Suppress("UNCHECKED_CAST")
    _weakMap.objectForKey(key) as V?
  }

  actual fun clear() = synchronized(lock) {
    _weakMap.removeAllObjects()
  }

  actual fun put(key: K, value: V): V? = synchronized(lock) {
    val oldValue = get(key)
    _weakMap.setObject(value, key)
    oldValue
  }

  actual fun remove(key: K): V? = synchronized(lock) {
    val oldValue = get(key)
    _weakMap.removeObjectForKey(key)
    oldValue
  }

}