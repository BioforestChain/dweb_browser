package org.dweb_browser.helper

import kotlin.coroutines.CoroutineContext

open class ChangeableHashSet<E> : MutableSet<E> by LinkedHashSet()

class ChangeableSet<E>(context: CoroutineContext = ioAsyncExceptionHandler) :
  ChangeableHashSet<E>() {
  private val changeable = Changeable(this, context)
  fun setContext(context: CoroutineContext) {
    changeable.context = context
  }

  val onChange get() = changeable.onChange

  suspend fun emitChange() = changeable.emitChange()

  override fun add(element: E) =
    super.add(element).also { if (it) changeable.emitChangeBackground() }

  override fun clear() = super.clear().also { changeable.emitChangeBackground() }
  override fun remove(element: E) =
    super.remove(element).also { if (it) changeable.emitChangeBackground() }

  override fun addAll(elements: Collection<E>) =
    super.addAll(elements).also { if (it) changeable.emitChangeBackground() }

  override fun removeAll(elements: Collection<E>) =
    super.removeAll(elements).also { if (it) changeable.emitChangeBackground() }

  override fun retainAll(elements: Collection<E>) =
    super.retainAll(elements).also { if (it) changeable.emitChangeBackground() }

  /** 重置 清空所有的事件监听，清空所有的数据  */
  fun reset() {
    changeable.clear()
    this.clear()
  }
}