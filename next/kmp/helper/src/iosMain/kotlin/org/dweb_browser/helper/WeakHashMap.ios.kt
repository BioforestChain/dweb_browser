package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import platform.Foundation.NSMapTable
import platform.Foundation.NSPointerFunctionsStrongMemory
import platform.Foundation.NSPointerFunctionsWeakMemory

actual class WeakHashMap<K : Any, V> {
  private val lock = SynchronizedObject()
  private val _weakMap = NSMapTable(
    keyOptions = NSPointerFunctionsWeakMemory,
    valueOptions = NSPointerFunctionsStrongMemory,
    capacity = 0u
  )

  actual fun containsKey(key: K): Boolean {
    return _weakMap.objectForKey(key) != null
  }

  actual fun get(key: K): V? = synchronized(lock) {
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