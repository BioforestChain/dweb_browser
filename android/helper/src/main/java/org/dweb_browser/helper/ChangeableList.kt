package org.dweb_browser.helper

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ChangeableList<T> : MutableList<T> {

  private val innerList = mutableListOf<T>()
  private val _changeSignal = Signal<MutableList<T>>()

  fun onChange(callback: Callback<MutableList<T>>) = _changeSignal.listen(callback)

  override var size: Int = innerList.size

  override fun clear() {
    innerList.clear()
    GlobalScope.launch(ioAsyncExceptionHandler) {
      _changeSignal.emit(innerList)
    }
  }

  override fun addAll(elements: Collection<T>): Boolean {
    val handle = innerList.addAll(elements)
    GlobalScope.launch(ioAsyncExceptionHandler) {
      _changeSignal.emit(innerList)
    }
    return handle
  }

  override fun addAll(index: Int, elements: Collection<T>): Boolean {
    val handle = innerList.addAll(index, elements)
    GlobalScope.launch(ioAsyncExceptionHandler) {
      _changeSignal.emit(innerList)
    }
    return handle
  }

  override fun add(index: Int, element: T) {
    innerList.add(index, element)
    GlobalScope.launch(ioAsyncExceptionHandler) {
      _changeSignal.emit(innerList)
    }
  }

  override fun add(element: T): Boolean {
    val handle = innerList.add(element)
    GlobalScope.launch(ioAsyncExceptionHandler) {
      _changeSignal.emit(innerList)
    }
    return handle
  }

  override fun get(index: Int): T {
    return innerList.get(index)
  }

  override fun isEmpty(): Boolean {
    return innerList.isEmpty()
  }

  override fun iterator(): MutableIterator<T> {
    return innerList.iterator()
  }

  override fun listIterator(): MutableListIterator<T> {
    return innerList.listIterator()
  }

  override fun listIterator(index: Int): MutableListIterator<T> {
    return innerList.listIterator(index)
  }

  override fun removeAt(index: Int): T {
    val item = innerList.removeAt(index)
    GlobalScope.launch(ioAsyncExceptionHandler) {
      _changeSignal.emit(innerList)
    }
    return item
  }

  override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
    return innerList.subList(fromIndex, toIndex)
  }

  override fun set(index: Int, element: T): T {
    val item = innerList.set(index, element)
    GlobalScope.launch(ioAsyncExceptionHandler) {
      _changeSignal.emit(innerList)
    }
    return item
  }

  override fun retainAll(elements: Collection<T>): Boolean {
    val boolean = innerList.retainAll(elements)
    GlobalScope.launch(ioAsyncExceptionHandler) {
      _changeSignal.emit(innerList)
    }
    return boolean
  }

  override fun removeAll(elements: Collection<T>): Boolean {
    val boolean = innerList.removeAll(elements)
    GlobalScope.launch(ioAsyncExceptionHandler) {
      _changeSignal.emit(innerList)
    }
    return boolean
  }

  override fun remove(element: T): Boolean {
    val boolean = innerList.remove(element)
    GlobalScope.launch(ioAsyncExceptionHandler) {
      _changeSignal.emit(innerList)
    }
    return boolean
  }

  override fun lastIndexOf(element: T): Int {
    return innerList.lastIndexOf(element)
  }

  override fun indexOf(element: T): Int {
    return innerList.indexOf(element)
  }

  override fun contains(element: T): Boolean {
    return innerList.contains(element)
  }

  override fun containsAll(elements: Collection<T>): Boolean {
    return innerList.containsAll(elements)
  }
}