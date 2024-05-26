package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

class SafeHashSet<T> : MutableSet<T> {
  val origin = mutableSetOf<T>()
  val lock = SynchronizedObject()
  inline fun <R> sync(block: MutableSet<T>.() -> R) = synchronized(lock) { origin.block() }
  override val size get() = sync { size }
  override fun clear() = sync { clear() }
  override fun isEmpty() = sync { isEmpty() }
  override fun containsAll(elements: Collection<T>) = sync { containsAll(elements) }
  override fun contains(element: T) = sync { contains(element) }
  override fun iterator(): MutableIterator<T> = sync { toMutableSet().iterator() }
  override fun retainAll(elements: Collection<T>) = sync { retainAll(elements) }
  override fun removeAll(elements: Collection<T>) = sync { removeAll(elements) }
  override fun remove(element: T) = sync { remove(element) }
  override fun addAll(elements: Collection<T>) = sync { addAll(elements) }
  override fun add(element: T) = sync { add(element) }

  override fun toString(): String {
    return origin.toString()
  }
}
