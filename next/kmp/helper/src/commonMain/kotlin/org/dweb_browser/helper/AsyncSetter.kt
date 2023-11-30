package org.dweb_browser.helper

import kotlin.reflect.KProperty

class AsyncSetter<T>(private var _value: T, val onChanged: suspend (T) -> Unit) {
  val value get() = _value
  operator fun getValue(thisRef: Any, property: KProperty<*>) = _value
  suspend fun set(newValue: T) {
    if (_value != newValue) {
      _value = newValue
      onChanged(newValue)
    }
  }
}