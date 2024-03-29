package org.dweb_browser.helper

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow

open class Observer<T>(initValue: T) {
  protected val state = MutableStateFlow(initValue)
  suspend fun emit(v: T) {
    state.emit(v)
  }

  suspend fun observe(cb: FlowCollector<T>): Nothing = state.collect(cb)
}

class SimpleObserver : Observer<Int>(0) {
  fun next() {
    state.value += 1
  }
}