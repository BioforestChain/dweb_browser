package org.dweb_browser.js_frontend.state_compose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.dweb_browser.js_common.network.socket.SocketData
import org.dweb_browser.js_common.state_compose.ComposeFlow
import org.dweb_browser.js_frontend.network.socket.Socket

/**
 * 扩展 ComposeFlow 添加 Socket 实现自动同步到 Server 和 接受Server 同步
 */
fun ComposeFlow.StateComposeFlow<*, *, *>.autoSyncToServerStart(
    socket: Socket,
){
    /**
     * 铜鼓给Server
     */
    var jobCollector: Job? = null
    CoroutineScope(Dispatchers.Default).launch {
        jobCollector = operationFlowCore.collectClientString{
            val socketData = SocketData(
                composeFlowId = id,
                data = it
            )
            val jsonStr = Json.encodeToString(socketData)
            socket.syncToServer(jsonStr)
        }
    }

    /**
     * 接受到Server同步过来的数据
     */
    socket.onMessage {
        val socketData = Json.decodeFromString<SocketData>(it)
        if(socketData.composeFlowId == id){
            val operationValueContainer = decodeFromString(socketData.data)
            CoroutineScope(Dispatchers.Default).launch {
                emitByServer(operationValueContainer.value, operationValueContainer.emitType)
            }
        }
    }
}

fun ComposeFlow.ListComposeFlow<*, *, *>.autoSyncToServerStart(
    socket: Socket,
){
    /**
     * 铜鼓给Server
     */
    var jobCollector: Job? = null
    CoroutineScope(Dispatchers.Default).launch {
        jobCollector = operationFlowCore.collectClientString{
            val socketData = SocketData(
                composeFlowId = id,
                data = it
            )
            val jsonStr = Json.encodeToString(socketData)
            socket.syncToServer(jsonStr)
        }
    }

    /**
     * 接受到Server同步过来的数据
     */
    socket.onMessage {
        val socketData = Json.decodeFromString<SocketData>(it)
        if(socketData.composeFlowId == id) {
            val operationValueContainer = decodeFromString(socketData.data)
            CoroutineScope(Dispatchers.Default).launch {
                emitByServer(operationValueContainer.value, operationValueContainer.emitType)
            }
        }
    }
}




