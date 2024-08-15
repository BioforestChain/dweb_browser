package org.dweb_browser.helper

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow

public open class Observer<T>(initValue: T) {
  protected val state: MutableStateFlow<T> = MutableStateFlow(initValue)
  public suspend fun emit(v: T) {
    state.emit(v)
  }

  public suspend fun observe(cb: FlowCollector<T>): Nothing = state.collect(cb)
}

public class SimpleObserver : Observer<Int>(0) {
  public fun next() {
    state.value += 1
  }
}