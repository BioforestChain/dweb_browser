package org.dweb_browser.js_common.view_model

import kotlinx.coroutines.flow.FlowCollector
import org.dweb_browser.js_common.state_compose.ComposeFlow
import org.dweb_browser.js_common.state_compose.operation.OperationValueContainer

//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import org.dweb_browser.js_common.state_compose.ComposeFlow
//
////
////class Flow<T: Any, ClosedReason: Any>(
////    val flow: ComposeFlow<T, ClosedReason>
////) {
////    fun emitByServerWithOperationValueContainerString(jsonString: String) {
////        val operationValueContainer = flow.decodeFromString(jsonString)
////        CoroutineScope(Dispatchers.Default).launch {
////            if (operationValueContainer.index != -1) {
////                flow.emitByServer(
////                    operationValueContainer.value, operationValueContainer.emitType
////                )
////            } else {
////                flow.emitByServer(
////                    operationValueContainer.value,
////                    operationValueContainer.emitType,
////                    operationValueContainer.index
////                )
////            }
////        }
////    }
////
////    fun emitByClientWithOperationValueContainerString(jsonString: String) {
////        val operationValueContainer = flow.decodeFromString(jsonString)
////        CoroutineScope(Dispatchers.Default).launch {
////            if (operationValueContainer.index != -1) {
////                flow.emitByServer(
////                    operationValueContainer.value, operationValueContainer.emitType
////                )
////            } else {
////                flow.emitByServer(
////                    operationValueContainer.value,
////                    operationValueContainer.emitType,
////                    operationValueContainer.index
////                )
////            }
////        }
////    }
////
////    companion object {
////        inline fun <reified T : Any, ClosedReason : Any> createStateFlow(): Flow<T, ClosedReason> {
////            return Flow(
////                flow = ComposeFlow.createStateComposeFlowInstance<T, ClosedReason>()
////            )
////        }
////
////        inline fun <reified T : Any, CloseReason : Any> createListFlow(): Flow<List<T>, CloseReason> {
////            return Flow(
////                flow = ComposeFlow.createListComposeFlowInstance<T, CloseReason>()
////            )
////        }
////    }
////}
//
//
//sealed class DataStateValue{
//    class Flow<T: Any, ClosedReason: Any>(
//        val flow: ComposeFlow<T, ClosedReason>
//    ): DataStateValue(){
//        fun emitByServerWithOperationValueContainerString(jsonString: String) {
//            val operationValueContainer = flow.decodeFromString(jsonString)
//            CoroutineScope(Dispatchers.Default).launch {
//                if (operationValueContainer.index != -1) {
//                    flow.emitByServer(
//                        operationValueContainer.value, operationValueContainer.emitType
//                    )
//                } else {
//                    flow.emitByServer(
//                        operationValueContainer.value,
//                        operationValueContainer.emitType,
//                        operationValueContainer.index
//                    )
//                }
//            }
//        }
//
//        fun emitByClientWithOperationValueContainerString(jsonString: String) {
//            val operationValueContainer = flow.decodeFromString(jsonString)
//            CoroutineScope(Dispatchers.Default).launch {
//                if (operationValueContainer.index != -1) {
//                    flow.emitByServer(
//                        operationValueContainer.value, operationValueContainer.emitType
//                    )
//                } else {
//                    flow.emitByServer(
//                        operationValueContainer.value,
//                        operationValueContainer.emitType,
//                        operationValueContainer.index
//                    )
//                }
//            }
//        }
//
//        companion object {
//            inline fun <reified T : Any, ClosedReason : Any> createStateFlow(): Flow<T, ClosedReason> {
//                return Flow(
//                    flow = ComposeFlow.createStateComposeFlowInstance<T, ClosedReason>()
//                )
//            }
//
//            inline fun <reified T : Any, CloseReason : Any> createListFlow(): Flow<List<T>, CloseReason> {
//                return Flow(
//                    flow = ComposeFlow.createListComposeFlowInstance<T, CloseReason>()
//                )
//            }
//        }
//    }
//
//    class ValueMap<T: Any>(val value: Map<T, DataStateValue>): DataStateValue()
//    companion object{
//        inline fun <reified T: Any, ClosedReason: Any>createValueStateFlowInstance(): Flow<T, ClosedReason>{
//            return Flow.createStateFlow<T, ClosedReason>()
//        }
//
//        inline fun <reified T: Any, ClosedReason: Any>createValueListFlowInstance(): Flow<List<T>, ClosedReason>{
//            return Flow.createListFlow<T, ClosedReason>()
//        }
//
////        fun createValueMap(value: DataState): ValueMap{
////            return ValueMap(
////                value = value
////            )
////        }
//    }
//
//}
//
//
//typealias DataState = Map<String, DataStateValue>


class Value<T: Any>(
    val value: T
)

sealed class DataStateValue<T: Any>(){
    abstract val value: T
    class StateValue<Item: Any, ClosedReason: Any >(
        override val value: ComposeFlow.StateComposeFlow<Item, ClosedReason>
    ): DataStateValue<ComposeFlow.StateComposeFlow<Item, ClosedReason>>()

    class ListValue<Item: Any, ClosedReason: Any>(
        override val value: ComposeFlow.ListComposeFlow<Item, ClosedReason>
    ): DataStateValue<ComposeFlow.ListComposeFlow<Item, ClosedReason>>()
    // TODO: 是否需要Map??? 
//    class MapValue<T: Map<String, DataStateValue<*>>>(override val value: T): DataStateValue<T>()

    companion object{
        inline fun <reified T: Any, ClosedReason: Any>createStateValue(): StateValue<T, ClosedReason>{
            return StateValue(value = ComposeFlow.createStateComposeFlowInstance<T, ClosedReason>())
        }

        inline fun <reified T: Any, ClosedReason: Any>createListValue(): ListValue<T, ClosedReason>{
            return ListValue(value = ComposeFlow.createListComposeFlowInstance<T, ClosedReason>())
        }

//         fun <T: Map<String, DataStateValue<*>>>createMapValue(value: T): MapValue<T>{
//            return MapValue(value)
//        }
    }
}



typealias DataState = Map<String, DataStateValue<*>>


