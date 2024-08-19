package org.dweb_browser.helper

import kotlinx.serialization.Serializable
import kotlin.coroutines.CoroutineContext

@Serializable
public data class ChangeState<K>(val adds: Set<K>, val updates: Set<K>, val removes: Set<K>)

public class ChangeableMap<K, V>(
  context: CoroutineContext = defaultAsyncExceptionHandler,
  private val origin: MutableMap<K, V> = mutableMapOf(),
) :
  MutableMap<K, V> by origin {
  public data class Changes<K, V>(
    val origin: ChangeableMap<K, V>, val adds: Set<K>, val updates: Set<K>, val removes: Set<K>,
  )

  private val changeable = Changeable(Changes(this, setOf(), setOf(), setOf()), context)
  public fun setContext(context: CoroutineContext) {
    changeable.context = context
  }

  public val onChange: Signal.Listener<Changes<K, V>> get() = changeable.onChange
  public suspend fun emitChange(): Unit = changeable.emitChange()
  public fun emitChangeBackground(
    adds: Set<K> = setOf(),
    updates: Set<K> = setOf(),
    removes: Set<K> = setOf(),
  ) {
    changeable.emitChangeBackground(Changes(this, adds, updates, removes))
  }

  override fun clear() {
    if (keys.size > 0) {
      val removes = keys.toSet()
      origin.clear().also { emitChangeBackground(removes) }
    }
  }

  override fun put(key: K, value: V): V? = origin.put(key, value).also {
    if (it != null) {// 有前置的值，那么就触发 update
      emitChangeBackground(updates = setOf(key))
    } else {// 没有前置的值，那么就触发 add
      emitChangeBackground(adds = setOf(key))
    }
  }

  public operator fun set(key: K, value: V) {
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
    origin.putAll(from).also { emitChangeBackground(updates, adds) }
  }

  override fun remove(key: K): V? =
    origin.remove(key)?.also { emitChangeBackground(removes = setOf(key)) }

  public fun remove(key: K, value: V): Boolean =
    (this as MutableMap<K, V>).remove(key, value)

  public fun delete(key: K): Boolean = remove(key) != null


  /** 重置 清空所有的事件监听，清空所有的数据
   * 注意，这里不会触发任何事件，如果有需要，请使用 clear ，然后再 reset
   */
  public fun reset() {
    changeable.clear()
    this.clear()
  }
}