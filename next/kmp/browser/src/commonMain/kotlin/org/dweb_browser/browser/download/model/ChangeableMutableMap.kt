package org.dweb_browser.browser.download.model

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import kotlin.coroutines.CoroutineContext

enum class ChangeableType {
  Add, Update, Remove, Clear, PutAll
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

  fun get(key: K) = cMaps[key]

  fun set(key: K, value: V) = put(key, value)

  suspend fun suspendForEach(each: suspend (K, V) -> Unit) =
    cMaps.forEach { (key, value) -> each(key, value) }

  fun forEach(each: (K, V) -> Unit) = cMaps.forEach { (key, value) -> each(key, value) }

  private fun emitBackground(type: ChangeableType, key: K? = null, value: V? = null) {
    CoroutineScope(context).launch {
      changeSignal.emit(Triple(type, key, value))
    }
  }

  private val changeSignal: Signal<Triple<ChangeableType, K?, V?>> = Signal()
  val onChange = changeSignal.toListener()
}