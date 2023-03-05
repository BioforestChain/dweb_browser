package info.bagen.rust.plaoc.microService.helper

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

open class Observer<T>(initValue: T) {
    protected val state = MutableStateFlow(initValue)
    protected val flow = state.asSharedFlow()
    suspend fun emit(v: T) {
        state.emit(v)
    }

    suspend fun observe(cb: FlowCollector<T>) {
        flow.collect(cb)
    }
}

class SimpleObserver : Observer<Int>(0) {
    fun next() {
        state.value += 1
    }
}