package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class ChangeableList<T>(private val scope: CoroutineScope = GlobalScope) : ArrayList<T>() {
  private val _changeSignal = Signal<ChangeableList<T>>()

  val onChange = _changeSignal.toListener()

  fun emitChange() = scope.launch(ioAsyncExceptionHandler) {
    _changeSignal.emit(this@ChangeableList)
  }


  override fun clear() {
    super.clear()
    emitChange()
  }

  override fun addAll(elements: Collection<T>): Boolean {
    val handle = super.addAll(elements)
    emitChange()
    return handle
  }

  override fun addAll(index: Int, elements: Collection<T>): Boolean {
    val handle = super.addAll(index, elements)
    emitChange()
    return handle
  }

  override fun add(index: Int, element: T) {
    super.add(index, element)
    emitChange()
  }

  override fun add(element: T): Boolean {
    val handle = super.add(element)
    emitChange()
    return handle
  }

  fun lastOrNull(): T? {
    return if (isEmpty()) null else this[size - 1]
  }

  override fun removeAt(index: Int): T {
    val item = super.removeAt(index)
    emitChange()
    return item
  }

  override fun set(index: Int, element: T): T {
    val item = super.set(index, element)
    emitChange()
    return item
  }

  override fun retainAll(elements: Collection<T>): Boolean {
    val boolean = super.retainAll(elements)
    if (boolean) {
      emitChange()
    }
    return boolean
  }

  override fun removeAll(elements: Collection<T>): Boolean {
    val boolean = super.removeAll(elements)
    if (boolean) {
      emitChange()
    }
    return boolean
  }

  override fun remove(element: T): Boolean {
    val boolean = super.remove(element)
    if (boolean) {
      emitChange()
    }
    return boolean
  }

}