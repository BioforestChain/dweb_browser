package org.dweb_browser.helper

import kotlin.coroutines.CoroutineContext

class ChangeableMap<K, V>(context: CoroutineContext = ioAsyncExceptionHandler) :
  LinkedHashMap<K, V>() {
  private val changeable = Changeable(this, context)
  val onChange = changeable.onChange
  val watch = changeable.watch
  suspend fun emitChange() = changeable.emitChange()
  fun emitChangeSync() = changeable.emitChangeSync()
  override fun clear() = super.clear().also { changeable.emitChangeSync() }
  override fun put(key: K, value: V) = super.put(key, value).also { changeable.emitChangeSync() }

  private operator fun set(key: K, value: V) {
    this.put(key, value)
  }

  override fun putAll(from: Map<out K, V>) = super.putAll(from).also { changeable.emitChangeSync() }
  override fun remove(key: K) =
    super.remove(key).also { if (it != null) changeable.emitChangeSync() }


  /** 重置 清空所有的事件监听，清空所有的数据  */
  fun reset() {
    changeable.signal.clear()
    this.clear()
  }
}