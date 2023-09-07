package org.dweb_browser.helper
import platform.Foundation.NSMapTable
import platform.Foundation.NSPointerFunctionsStrongMemory
import platform.Foundation.NSPointerFunctionsWeakMemory

actual class WeakHashMap<K : Any, V>   {
  private val _weakMap = NSMapTable(keyOptions = NSPointerFunctionsWeakMemory, valueOptions = NSPointerFunctionsStrongMemory,capacity= 0u)
  actual fun containsKey(key: K): Boolean {
    return _weakMap.objectForKey(key)!=null
  }

  actual fun get(key: K): V? {
    return _weakMap.objectForKey(key) as V?
  }

  actual fun clear() {
    _weakMap.removeAllObjects()
  }

  actual fun put(key: K, value: V): V? {
   val oldValue = get(key)
    _weakMap.setObject(value,key)
    return oldValue
  }

  actual fun remove(key: K): V? {
    val oldValue = get(key)
    _weakMap.removeObjectForKey(key)
    return oldValue
  }

}