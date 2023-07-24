package info.bagen.dwebbrowser.util

import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.runBlockingCatching

class ChangeableMap<K,V>() :MutableMap<K,V>{
  private val innerMap = HashMap<K,V>()
  private val _changeSignal = Signal<HashMap<K,V>>()

  fun onChange(callback: Callback<HashMap<K,V>>) = _changeSignal.listen(callback)


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
    runBlockingCatching {
      _changeSignal.emit(innerMap)
    }
    innerMap.clear()
  }

  override fun put(key: K, value: V): V? {
    runBlockingCatching {
      _changeSignal.emit(innerMap)
    }
    return innerMap.put(key, value)
  }

  override fun putAll(from: Map<out K, V>) {
    runBlockingCatching {
      _changeSignal.emit(innerMap)
    }
    innerMap.putAll(from)
  }

  override fun remove(key: K): V? {
    runBlockingCatching {
      _changeSignal.emit(innerMap)
    }
    return innerMap.remove(key)
  }
}