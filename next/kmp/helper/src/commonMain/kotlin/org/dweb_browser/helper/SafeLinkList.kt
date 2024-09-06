package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

public class SafeLinkList<T>(public val origin: MutableList<T> = mutableListOf()) : MutableList<T> {
  public val lock: SynchronizedObject = SynchronizedObject()
  public inline fun <R> sync(block: MutableList<T>.() -> R): R =
    synchronized(lock) { origin.block() }

  override val size: Int get() = sync { origin.size }
  override fun clear(): Unit = sync { clear() }

  override fun addAll(elements: Collection<T>): Boolean = sync { addAll(elements) }

  override fun addAll(index: Int, elements: Collection<T>): Boolean =
    sync { addAll(index, elements) }

  override fun add(index: Int, element: T): Unit = sync { add(index, element) }


  override fun add(element: T): Boolean = sync { add(element) }


  override fun get(index: Int): T = sync { origin[index] }

  override fun isEmpty(): Boolean = sync { isEmpty() }

  override fun iterator(): MutableIterator<T> = sync { iterator() }

  override fun listIterator(): MutableListIterator<T> = sync { listIterator() }

  override fun listIterator(index: Int): MutableListIterator<T> = sync { listIterator(index) }

  override fun removeAt(index: Int): T = sync { removeAt(index) }

  override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
    sync { subList(fromIndex, toIndex) }

  override fun set(index: Int, element: T): T = sync {
    origin.set(index, element)
  }

  override fun retainAll(elements: Collection<T>): Boolean = sync {
    origin.retainAll(elements)
  }

  override fun removeAll(elements: Collection<T>): Boolean = sync {
    origin.removeAll(elements)
  }

  override fun remove(element: T): Boolean = sync {
    origin.remove(element)
  }

  override fun lastIndexOf(element: T): Int = sync {
    origin.lastIndexOf(element)
  }

  override fun indexOf(element: T): Int = sync {
    origin.indexOf(element)
  }

  override fun containsAll(elements: Collection<T>): Boolean = sync {
    origin.containsAll(elements)
  }

  override fun contains(element: T): Boolean = sync {
    origin.contains(element)
  }

  public fun toList(): List<T> = sync { toList() }
  public fun toMutableList(): MutableList<T> = sync { toMutableList() }
  public fun toHashSet(): HashSet<T> = sync { toHashSet() }

  public fun firstOrNull(): T? = sync { firstOrNull() }

  override fun toString(): String {
    return sync { origin.toString() }
  }
}