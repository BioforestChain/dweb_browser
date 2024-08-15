package org.dweb_browser.helper

import kotlin.reflect.KProperty

public class AsyncSetter<T>(private var _value: T, public val onChanged: suspend (T) -> Unit) {
  public val value: T get() = _value
  public operator fun getValue(thisRef: Any, property: KProperty<*>): T = _value
  public suspend fun set(newValue: T) {
    if (_value != newValue) {
      _value = newValue
      onChanged(newValue)
    }
  }
}