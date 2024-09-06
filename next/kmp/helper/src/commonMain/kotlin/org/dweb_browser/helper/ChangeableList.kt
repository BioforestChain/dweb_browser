package org.dweb_browser.helper

import kotlin.coroutines.CoroutineContext

public open class ChangeableListBase<T> : MutableList<T> by ArrayList()

public class ChangeableList<T>(context: CoroutineContext = defaultAsyncExceptionHandler) :
  ChangeableListBase<T>() {
  private val changeable = Changeable(this, context)
  public fun setContext(context: CoroutineContext) {
    changeable.context = context
  }

  public val onChange: Signal.Listener<ChangeableList<T>> get() = changeable.onChange
  public suspend fun emitChange(): Unit = changeable.emitChange()

  override fun clear() {
    return super.clear().also { changeable.emitChangeBackground() }
  }

  override fun addAll(elements: Collection<T>): Boolean {
    return super.addAll(elements).also { if (it) changeable.emitChangeBackground() }
  }

  override fun addAll(index: Int, elements: Collection<T>): Boolean {
    return super.addAll(index, elements).also { if (it) changeable.emitChangeBackground() }
  }

  override fun add(index: Int, element: T) {
    return super.add(index, element).also { changeable.emitChangeBackground() }
  }

  override fun add(element: T): Boolean {
    return super.add(element).also { if (it) changeable.emitChangeBackground() }
  }

  public fun lastOrNull(): T? {
    return if (isEmpty()) null else this[size - 1]
  }

  override fun removeAt(index: Int): T {
    return super.removeAt(index).also { changeable.emitChangeBackground() }
  }

  override fun set(index: Int, element: T): T {
    return super.set(index, element).also { changeable.emitChangeBackground() }
  }

  override fun retainAll(elements: Collection<T>): Boolean {
    return super.retainAll(elements).also { if (it) changeable.emitChangeBackground() }
  }

  override fun removeAll(elements: Collection<T>): Boolean {
    return super.removeAll(elements).also { if (it) changeable.emitChangeBackground() }
  }

  override fun remove(element: T): Boolean {
    return super.remove(element).also { if (it) changeable.emitChangeBackground() }
  }

  /** 重置 清空所有的事件监听，清空所有的数据  */
  public fun reset() {
    changeable.clear()
    this.clear()
  }
}