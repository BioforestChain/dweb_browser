package org.dweb_browser.browser.download.model

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.getOrDefault
import org.dweb_browser.helper.ioAsyncExceptionHandler
import kotlin.coroutines.CoroutineContext

enum class ChangeableType {
  Add, Remove, Clear, PutAll
  ;
}

class ChangeableMutableMap<K, V>(
  val cMaps: MutableMap<K, V> = mutableStateMapOf(),
  private val context: CoroutineContext = ioAsyncExceptionHandler
) {
  fun putAll(maps: MutableMap<K, V>) {
    cMaps.putAll(maps)
    emitBackground(ChangeableType.PutAll)
  }

  fun put(key: K, value: V) {
    remove(key) // 为了确保后面接收onChange的时候，如果已存在的，会先收到一个移除的消息
    cMaps[key] = value
    emitBackground(ChangeableType.Add, key, value)
  }

  fun remove(key: K): V? {
    val value = cMaps.remove(key)
    value?.let { emitBackground(ChangeableType.Remove, key, value) }
    return value
  }

  fun clear() {
    cMaps.clear()
    emitBackground(ChangeableType.Clear)
  }

  fun containsKey(key: K) = cMaps.containsKey(key)

  fun isEmpty() = cMaps.isEmpty()

  operator fun get(key: K) = cMaps[key]

  val size get() = cMaps.size

  suspend fun suspendForEach(each: suspend (K, V) -> Unit) =
    cMaps.forEach { (key, value) -> each(key, value) }

  suspend fun forEach(each: suspend (K, V) -> Unit) =
    cMaps.forEach { (key, value) -> each(key, value) }

  private fun emitBackground(type: ChangeableType, key: K? = null, value: V? = null) {
    CoroutineScope(context).launch {
      changeSignal.emit(Triple(type, key, value))
    }
  }

  private val changeSignal: Signal<Triple<ChangeableType, K?, V?>> = Signal()
  val onChange = changeSignal.toListener()

  fun toMutableList() = cMaps.values.toList()

  fun getOrPut(key: K, defaultValue: () -> V) = cMaps.getOrPut(key, defaultValue)
  fun getOrDefault(key: K, defaultValue: () -> V) = cMaps.getOrDefault(key, defaultValue)
  fun getOrElse(key: K, defaultValue: () -> V) = cMaps.getOrElse(key, defaultValue)
}