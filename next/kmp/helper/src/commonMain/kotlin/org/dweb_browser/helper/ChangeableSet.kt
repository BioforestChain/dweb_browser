package org.dweb_browser.helper

import kotlin.coroutines.CoroutineContext


public class ChangeableSet<E>(
  public val source: MutableSet<E> = mutableSetOf(),
  context: CoroutineContext = defaultAsyncExceptionHandler,
) :
  MutableSet<E> by source {
  private val changeable = Changeable(this, context)
  public fun setContext(context: CoroutineContext) {
    changeable.context = context
  }

  public val onChange: Signal.Listener<ChangeableSet<E>> get() = changeable.onChange

  public suspend fun emitChange(): Unit = changeable.emitChange()

  override fun add(element: E): Boolean =
    source.add(element).also { if (it) changeable.emitChangeBackground() }

  override fun clear(): Unit = source.clear().also { changeable.emitChangeBackground() }
  override fun remove(element: E): Boolean =
    source.remove(element).also { if (it) changeable.emitChangeBackground() }

  override fun addAll(elements: Collection<E>): Boolean =
    source.addAll(elements).also { if (it) changeable.emitChangeBackground() }

  override fun removeAll(elements: Collection<E>): Boolean =
    source.removeAll(elements).also { if (it) changeable.emitChangeBackground() }

  override fun retainAll(elements: Collection<E>): Boolean =
    source.retainAll(elements).also { if (it) changeable.emitChangeBackground() }

  /** 重置 清空所有的事件监听，清空所有的数据  */
  public fun reset() {
    changeable.clear()
    this.clear()
  }
}