package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

class SafeHashMap<K, V> : MutableMap<K, V> {
  val origin = mutableMapOf<K, V>()
  val lock = SynchronizedObject()
  inline fun <T> sync(block: MutableMap<K, V>.() -> T) = synchronized(lock) { origin.block() }
  override val size get() = sync { size }
  override val entries get() = sync { entries.toMutableSet() }
  override val keys get() = sync { keys.toMutableSet() }
  override val values get() = sync { values.toMutableSet() }
  override fun clear() = sync { clear() }

  override fun isEmpty() = sync { isEmpty() }

  override fun remove(key: K) = sync { remove(key) }
  fun remove(key: K, value: V) = sync { remove(key, value) }

  override fun putAll(from: Map<out K, V>) = sync { putAll(from) }

  override fun put(key: K, value: V) = sync { put(key, value) }

  override fun get(key: K) = sync { get(key) }

  override fun containsValue(value: V) = sync { containsValue(value) }

  override fun containsKey(key: K) = sync { containsKey(key) }
  inline fun getOrPut(key: K, defaultValue: () -> V) = sync { getOrPut(key, defaultValue) }
  inline fun getOrDefault(key: K, defaultValue: V) = sync { getOrDefault(key, defaultValue) }
  inline fun getOrElse(key: K, defaultValue: () -> V) = sync { getOrElse(key, defaultValue) }

  override fun toString(): String {
    return origin.toString()
  }
}