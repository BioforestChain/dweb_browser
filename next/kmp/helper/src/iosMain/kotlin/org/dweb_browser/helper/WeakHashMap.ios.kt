package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import platform.Foundation.NSMapTable


public actual class WeakHashMap<K : Any, V> {
  private val lock = SynchronizedObject()
  private val _weakMap = NSMapTable.weakToStrongObjectsMapTable()

  public actual fun containsKey(key: K): Boolean {
    return _weakMap.objectForKey(key) != null
  }

  public actual fun get(key: K): V? = synchronized(lock) {
    @Suppress("UNCHECKED_CAST")
    _weakMap.objectForKey(key) as V?
  }

  public actual fun clear(): Unit = synchronized(lock) {
    _weakMap.removeAllObjects()
  }

  public actual fun put(
    key: K,
    value: V,
  ): V? = synchronized(lock) {
    val oldValue = get(key)
    _weakMap.setObject(value, key)
    oldValue
  }

  public actual fun remove(key: K): V? = synchronized(lock) {
    val oldValue = get(key)
    _weakMap.removeObjectForKey(key)
    oldValue
  }

}