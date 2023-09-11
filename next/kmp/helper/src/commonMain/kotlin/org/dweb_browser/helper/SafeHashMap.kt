package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

class SafeHashMap<K, V> : MutableMap<K, V> {
  val origin = mutableMapOf<K, V>()
  val lock = SynchronizedObject()
  inline fun <T> sync(block: () -> T) = synchronized(lock, block)
  override val size get() = sync { origin.size }
  override val entries get() = sync { origin.entries.toMutableSet() }
  override val keys get() = sync { origin.keys.toMutableSet() }
  override val values get() = sync { origin.values.toMutableSet() }
  override fun clear() = sync { origin.clear() }

  override fun isEmpty() = sync { origin.isEmpty() }

  override fun remove(key: K) = sync { origin.remove(key) }

  override fun putAll(from: Map<out K, V>) = sync { origin.putAll(from) }

  override fun put(key: K, value: V) = sync { origin.put(key, value) }

  override fun get(key: K) = sync { origin.get(key) }

  override fun containsValue(value: V) = sync { origin.containsValue(value) }

  override fun containsKey(key: K) = sync { origin.containsKey(key) }
  inline fun getOrPut(key: K, defaultValue: () -> V) = sync { origin.getOrPut(key, defaultValue) }
  inline fun getOrDefault(key: K, defaultValue: V) = sync { origin.getOrDefault(key, defaultValue) }
  inline fun getOrElse(key: K,  defaultValue: () -> V) = sync { origin.getOrElse(key, defaultValue) }
}