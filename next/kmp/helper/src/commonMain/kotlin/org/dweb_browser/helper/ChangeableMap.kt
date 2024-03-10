package org.dweb_browser.helper

import kotlinx.serialization.Serializable
import kotlin.coroutines.CoroutineContext

@Serializable
data class ChangeState<K>(val adds: Set<K>, val updates: Set<K>, val removes: Set<K>);

/// TODO：LinkedHashMap 无法使用，会提示final type
open class ChangeableHashMap<K, V> : MutableMap<K, V> by LinkedHashMap()
class ChangeableMap<K, V>(context: CoroutineContext = ioAsyncExceptionHandler) :
  ChangeableHashMap<K, V>() {
  data class Changes<K, V>(
    val origin: ChangeableMap<K, V>, val adds: Set<K>, val updates: Set<K>, val removes: Set<K>
  )

  private val changeable = Changeable(Changes(this, setOf(), setOf(), setOf()), context)
  fun setContext(context: CoroutineContext) {
    changeable.context = context
  }

  val onChange get() = changeable.onChange
  suspend fun emitChange() = changeable.emitChange()
  fun emitChangeBackground(
    adds: Set<K> = setOf(),
    updates: Set<K> = setOf(),
    removes: Set<K> = setOf()
  ) {
    changeable.emitChangeBackground(Changes(this, adds, updates, removes))
  }

  override fun clear() {
    if (keys.size > 0) {
      val removes = keys.toSet()
      super.clear().also { emitChangeBackground(removes) }
    }
  }

  override fun put(key: K, value: V) = super.put(key, value).also {
    if (it != null) {// 有前置的值，那么就触发 update
      emitChangeBackground(updates = setOf(key))
    } else {// 没有前置的值，那么就触发 add
      emitChangeBackground(adds = setOf(key))
    }
  }

  operator fun set(key: K, value: V) {
    this.put(key, value)
  }

  override fun putAll(from: Map<out K, V>) {
    val updates = mutableSetOf<K>()
    val adds = mutableSetOf<K>()
    for (key in from.keys) {
      if (contains(key)) {
        updates.add(key)
      } else {
        adds.add(key)
      }
    }
    super.putAll(from).also { emitChangeBackground(updates, adds) }
  }

  override fun remove(key: K) =
    super.remove(key)?.also { emitChangeBackground(removes = setOf(key)) }

  fun remove(key: K, value: V) =
    (this as MutableMap<K, V>).remove(key, value)

  fun delete(key: K) = remove(key) != null


  /** 重置 清空所有的事件监听，清空所有的数据
   * 注意，这里不会触发任何事件，如果有需要，请使用 clear ，然后再 reset
   */
  fun reset() {
    changeable.clear()
    this.clear()
  }
}