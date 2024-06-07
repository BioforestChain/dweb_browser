package org.dweb_browser.helper

import kotlin.coroutines.CoroutineContext


class ChangeableSet<E>(
  val source: MutableSet<E> = mutableSetOf(),
  context: CoroutineContext = defaultAsyncExceptionHandler,
) :
  MutableSet<E> by source {
  private val changeable = Changeable(this, context)
  fun setContext(context: CoroutineContext) {
    changeable.context = context
  }

  val onChange get() = changeable.onChange

  suspend fun emitChange() = changeable.emitChange()

  override fun add(element: E) =
    source.add(element).also { if (it) changeable.emitChangeBackground() }

  override fun clear() = source.clear().also { changeable.emitChangeBackground() }
  override fun remove(element: E) =
    source.remove(element).also { if (it) changeable.emitChangeBackground() }

  override fun addAll(elements: Collection<E>) =
    source.addAll(elements).also { if (it) changeable.emitChangeBackground() }

  override fun removeAll(elements: Collection<E>) =
    source.removeAll(elements).also { if (it) changeable.emitChangeBackground() }

  override fun retainAll(elements: Collection<E>) =
    source.retainAll(elements).also { if (it) changeable.emitChangeBackground() }

  /** 重置 清空所有的事件监听，清空所有的数据  */
  fun reset() {
    changeable.clear()
    this.clear()
  }
}