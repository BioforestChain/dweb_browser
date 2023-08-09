package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

class Observable<K : Any> {
  private val changeSignal = Signal<K>();
  val onChange = changeSignal.toListener()

  class Observer<K : Any, T>(
    private val ob: Observable<K>,
    val key: K,
    var value: T,
  ) {
    operator fun setValue(thisRef: Any, property: KProperty<*>, input: T) {
      if (input != value) {
        value = input
        CoroutineScope(ioAsyncExceptionHandler).launch {
          ob.changeSignal.emit(key)
        }
      }
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>) = value
  }

  fun <T> observe(key: K, initValue: T) = Observer(this, key, initValue)
}