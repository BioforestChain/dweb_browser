package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

class Observable<K : Any> {
  data class Change<K, V>(val key: K, val newValue: V, val oldValue: V)

  val changeSignal = Signal<Change<K, *>>();
  val onChange = changeSignal.toListener()

  class Observer<K : Any, T>(
    private val ob: Observable<K>,
    val key: K,
    var value: T,
  ) {
    operator fun setValue(thisRef: Any, property: KProperty<*>, newValue: T) {
      if (newValue != value) {
        val oldValue = value
        value = newValue
        CoroutineScope(ioAsyncExceptionHandler).launch {
          ob.changeSignal.emit(Change(key, newValue, oldValue))
        }
      }
    }

    suspend fun set(newValue: T) {
      if (newValue != value) {
        val oldValue = value
        value = newValue
        CoroutineScope(ioAsyncExceptionHandler).launch {
          ob.changeSignal.emit(Change(key, newValue, oldValue))
        }
      }
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>) = value
  }

  fun <T> observe(key: K, initValue: T) = Observer(this, key, initValue)
}