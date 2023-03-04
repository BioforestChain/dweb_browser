package info.bagen.rust.plaoc.microService.helper

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

class Observer<T>(initValue: T) {
    private val state = MutableStateFlow(initValue)
    private val flow = state.asSharedFlow()
    suspend fun emit(v: T) {
        state.emit(v)
    }

    suspend fun observe(cb: FlowCollector<T>) {
        flow.collect(cb)
    }
}