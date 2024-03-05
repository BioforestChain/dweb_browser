package org.dweb_browser.js_common.state_compose.operation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.dweb_browser.js_common.state_compose.state.EmitType
import org.dweb_browser.js_common.state_compose.operation.OperationValueContainer
import org.dweb_browser.js_common.state_compose.serialization.Serialization

interface IOperationFlow<T: Any>{
    val serialization: Serialization<OperationValueContainer<T>>
    var currentStateFlowValue: T?
    // 用来保存操作方式的flow
    val stateOperationFlow: MutableSharedFlow<OperationValueContainer<T>>

    suspend fun emit(v: T, emitType: EmitType){
        // index == -1 表示没有 index
        stateOperationFlow.emit(OperationValueContainer(-1, v, emitType))
    }

    suspend fun emit(v: T, emitType: EmitType, index: Int){
        stateOperationFlow.emit(OperationValueContainer(index, v, emitType))
    }
    suspend fun emitReplace(v: T) = emit(v, EmitType.REPLACE)
}
