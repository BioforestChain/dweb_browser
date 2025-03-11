package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

public class SafeHashMap<K, V>(public val origin: MutableMap<K, V> = mutableMapOf()) :
  MutableMap<K, V> {
  public val lock: SynchronizedObject = SynchronizedObject()
  public inline fun <T> sync(block: MutableMap<K, V>.() -> T): T =
    synchronized(lock) { origin.block() }

  override val size: Int get() = sync { size }
  override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
    get() = sync {
      entries.toSet().toMutableSet()
    }
  override val keys: MutableSet<K> get() = sync { keys.toSet().toMutableSet() }
  override val values: MutableSet<V> get() = sync { values.toSet().toMutableSet() }
  override fun clear(): Unit = sync { clear() }

  override fun isEmpty(): Boolean = sync { isEmpty() }

  override fun remove(key: K): V? = sync { remove(key) }
  public fun remove(key: K, value: V): Boolean = sync { remove(key, value) }

  override fun putAll(from: Map<out K, V>): Unit = sync { putAll(from) }

  override fun put(key: K, value: V): V? = sync { put(key, value) }

  override fun get(key: K): V? = sync { get(key) }

  override fun containsValue(value: V): Boolean = sync { containsValue(value) }

  override fun containsKey(key: K): Boolean = sync { containsKey(key) }
  public inline fun getOrPut(key: K, defaultValue: () -> V): V =
    sync { getOrPut(key, defaultValue) }

  public fun getOrDefault(key: K, defaultValue: V): V = sync { getOrDefault(key, defaultValue) }
  public inline fun getOrElse(key: K, defaultValue: () -> V): V =
    sync { getOrElse(key, defaultValue) }

  override fun toString(): String {
    return origin.toString()
  }
}