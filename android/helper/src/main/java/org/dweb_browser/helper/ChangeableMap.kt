package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ChangeableMap<K, V>(private val scope: CoroutineScope = GlobalScope) : MutableMap<K, V> {
  private val innerMap = HashMap<K, V>()
  private val _changeSignal = Signal<HashMap<K, V>>()

  val onChange = _changeSignal.toListener()

  fun emitChange() = scope.launch(ioAsyncExceptionHandler) {
    _changeSignal.emit(innerMap)
  }


  override val size: Int
    get() = innerMap.size

  override fun containsKey(key: K): Boolean {
    return innerMap.containsKey(key)
  }

  override fun containsValue(value: V): Boolean {
    return innerMap.containsValue(value)
  }

  override fun get(key: K): V? {
    return innerMap[key]
  }

  override fun isEmpty(): Boolean {
    return innerMap.isEmpty()
  }

  override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
    get() = innerMap.entries

  override val keys: MutableSet<K>
    get() = innerMap.keys

  override val values: MutableCollection<V>
    get() = innerMap.values

  override fun clear() {
    innerMap.clear()
    emitChange()
  }

  override fun put(key: K, value: V): V? {
    val item = innerMap.put(key, value)
    emitChange()
    return item
  }

  override fun putAll(from: Map<out K, V>) {
    innerMap.putAll(from)
    emitChange()
  }

  override fun remove(key: K): V? {
    val item = innerMap.remove(key)
    emitChange()
    return item
  }

  /** 重置 清空所有的事件监听，清空所有的数据  */
  fun reset() {
    _changeSignal.clear()
    this.clear()
  }
}