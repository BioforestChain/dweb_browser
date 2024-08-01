package org.dweb_browser.helper

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KProperty


operator fun <T> MutableStateFlow<T>.setValue(
  target: Any,
  property: KProperty<*>,
  t: T,
) {
  value = t
}


operator fun <T> StateFlow<T>.getValue(target: Any, property: KProperty<*>): T {
  return value
}