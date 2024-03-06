package org.dweb_browser.js_frontend.state_compose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.dweb_browser.js_common.network.socket.SocketData
import org.dweb_browser.js_common.state_compose.ComposeFlow
import org.dweb_browser.js_frontend.network.socket.Socket

/**
 * 自动根据服务端发送过来的操作，完成状态同步
 */
fun ComposeFlow.StateComposeFlow<*,*,*>.autoSyncOperationFromServer(
    socket: Socket
): Job{
    val job = Job()
    CoroutineScope(Dispatchers.Default + job).launch {
        socket.messageFlow.collect{
            val socketData = Json.decodeFromString<SocketData>(it)
            if(socketData.composeFlowId == id){
                val operationValueContainer = decodeFromString(socketData.data)
                CoroutineScope(Dispatchers.Default).launch {
                    emitByServer(operationValueContainer.value, operationValueContainer.emitType)
                }
            }
        }
    }
    return job
}

/**
 * 自动根据服务端发送过来的操作，完成状态同步
 */
fun ComposeFlow.ListComposeFlow<*,*,*>.autoSyncOperationFromServer(
    socket: Socket
): Job{
    val job = Job()
    CoroutineScope(Dispatchers.Default + job).launch {
        socket.messageFlow.collect{
            val socketData = Json.decodeFromString<SocketData>(it)
            if(socketData.composeFlowId == id){
                val operationValueContainer = decodeFromString(socketData.data)
                CoroutineScope(Dispatchers.Default).launch {
                    emitByServer(operationValueContainer.value, operationValueContainer.emitType)
                }
            }
        }
    }
    return job
}