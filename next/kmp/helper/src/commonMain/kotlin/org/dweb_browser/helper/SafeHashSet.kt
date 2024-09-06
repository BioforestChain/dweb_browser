package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

@Suppress("ConvertArgumentToSet")
public class SafeHashSet<T> : MutableSet<T> {
  public val origin: MutableSet<T> = mutableSetOf()
  public val lock: SynchronizedObject = SynchronizedObject()
  public inline fun <R> sync(block: MutableSet<T>.() -> R): R =
    synchronized(lock) { origin.block() }

  override val size: Int get() = sync { size }
  override fun clear(): Unit = sync { clear() }
  override fun isEmpty(): Boolean = sync { isEmpty() }
  override fun containsAll(elements: Collection<T>): Boolean = sync { containsAll(elements) }
  override fun contains(element: T): Boolean = sync { contains(element) }
  override fun iterator(): MutableIterator<T> = sync { toMutableSet().iterator() }
  override fun retainAll(elements: Collection<T>): Boolean = sync { retainAll(elements) }
  override fun removeAll(elements: Collection<T>): Boolean = sync { removeAll(elements) }
  override fun remove(element: T): Boolean = sync { remove(element) }
  override fun addAll(elements: Collection<T>): Boolean = sync { addAll(elements) }
  override fun add(element: T): Boolean = sync { add(element) }

  override fun toString(): String {
    return origin.toString()
  }
}
