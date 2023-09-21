package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

class SafeHashSet<T> : MutableSet<T> {
  private val origin = mutableSetOf<T>()
  private val lock = SynchronizedObject()
  private inline fun <T> sync(block: () -> T) = synchronized(lock, block)
  override val size get() = sync { origin.size }
  override fun clear() = sync { origin.clear() }
  override fun isEmpty() = sync { origin.isEmpty() }
  override fun containsAll(elements: Collection<T>) = sync { origin.containsAll(elements) }
  override fun contains(element: T) = sync { origin.contains(element) }
  override fun iterator(): MutableIterator<T> = sync { origin.toMutableSet().iterator() }
  override fun retainAll(elements: Collection<T>) = sync { origin.retainAll(elements) }
  override fun removeAll(elements: Collection<T>) = sync { origin.removeAll(elements) }
  override fun remove(element: T) = sync { origin.remove(element) }
  override fun addAll(elements: Collection<T>) = sync { origin.addAll(elements) }
  override fun add(element: T) = sync { origin.add(element) }
}