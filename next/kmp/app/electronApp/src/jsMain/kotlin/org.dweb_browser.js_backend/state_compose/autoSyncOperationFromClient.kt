package org.dweb_browser.js_backend.state_compose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.dweb_browser.js_backend.view_model.ViewModelSocket
import org.dweb_browser.js_common.network.socket.SocketData
import org.dweb_browser.js_common.state_compose.ComposeFlow

/**
 * 通过从客户端发送过来的操作
 */
fun ComposeFlow.StateComposeFlow<*, *, *>.autoSyncOperationFromClient(
    viewModelSocket: ViewModelSocket
) {
    viewModelSocket.onData {
        val socketData = Json.decodeFromString<SocketData>(it)
        console.log("socketData.composeFlowId", socketData.composeFlowId, id)
        when (socketData.composeFlowId) {
            id -> CoroutineScope(Dispatchers.Default).launch {
                val operationValueContainer =
                    operationFlowCore.serialization.decodeFromString(socketData.data)
                console.log(
                    "emitByClient", operationValueContainer.value, operationValueContainer.emitType
                )
                emitByClient(operationValueContainer.value, operationValueContainer.emitType)
            }
        }
    }
}

/**
 * 通过从客户端发送过来的操作
 */
fun ComposeFlow.ListComposeFlow<*, *, *>.autoSyncOperationFromClient(
    viewModelSocket: ViewModelSocket
) {
    viewModelSocket.onData {
        val socketData = Json.decodeFromString<SocketData>(it)
        when (socketData.composeFlowId) {
            id -> CoroutineScope(Dispatchers.Default).launch {
                val operationValueContainer =
                    operationFlowCore.serialization.decodeFromString(socketData.data)
                emitByClient(operationValueContainer.value, operationValueContainer.emitType)
            }

            else -> {}
        }
    }
}