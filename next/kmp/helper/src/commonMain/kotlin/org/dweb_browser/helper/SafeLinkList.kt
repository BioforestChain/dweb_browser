package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

class SafeLinkList<T>(val origin: MutableList<T> = mutableListOf()) : MutableList<T> {
  val lock = SynchronizedObject()
  inline fun <R> sync(block: MutableList<T>.() -> R) = synchronized(lock) { origin.block() }
  override val size get() = sync { origin.size }
  override fun clear() = sync { clear() }

  override fun addAll(elements: Collection<T>) = sync { addAll(elements) }

  override fun addAll(index: Int, elements: Collection<T>) = sync { addAll(index, elements) }

  override fun add(index: Int, element: T) = sync { add(index, element) }


  override fun add(element: T) = sync { add(element) }


  override fun get(index: Int): T = sync { origin[index] }

  override fun isEmpty() = sync { isEmpty() }

  override fun iterator() = sync { iterator() }

  override fun listIterator() = sync { listIterator() }

  override fun listIterator(index: Int) = sync { listIterator(index) }

  override fun removeAt(index: Int) = sync { removeAt(index) }

  override fun subList(fromIndex: Int, toIndex: Int) = sync { subList(fromIndex, toIndex) }

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

  fun toList() = sync { toList() }
  fun toMutableList() = sync { toMutableList() }
  fun toHashSet() = sync { toHashSet() }

  fun firstOrNull() = sync { firstOrNull() }

  override fun toString(): String {
    return sync { origin.toString() }
  }
}