package org.dweb_browser.helper

import kotlin.coroutines.CoroutineContext

class ChangeableSet<E>(context: CoroutineContext = ioAsyncExceptionHandler) :
  LinkedHashSet<E>() {
  private val changeable = Changeable(this, context)
  val onChange = changeable.onChange
  val watch = changeable.watch
  suspend fun emitChange() = changeable.emitChange()

  override fun add(element: E) = super.add(element).also { if (it) changeable.emitChangeSync() }
  override fun clear() = super.clear().also { changeable.emitChangeSync() }
  override fun remove(element: E) =
    super.remove(element).also { if (it) changeable.emitChangeSync() }

  override fun addAll(elements: Collection<E>) =
    super.addAll(elements).also { if (it) changeable.emitChangeSync() }

  override fun removeAll(elements: Collection<E>) =
    super.removeAll(elements).also { if (it) changeable.emitChangeSync() }

  override fun retainAll(elements: Collection<E>) =
    super.retainAll(elements).also { if (it) changeable.emitChangeSync() }

  /** 重置 清空所有的事件监听，清空所有的数据  */
  fun reset() {
    changeable.signal.clear()
    this.clear()
  }
}