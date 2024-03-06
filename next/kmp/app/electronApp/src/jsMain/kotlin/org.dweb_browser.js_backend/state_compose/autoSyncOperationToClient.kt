package org.dweb_browser.js_backend.state_compose

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.js_backend.view_model.ViewModelSocket
import org.dweb_browser.js_common.network.socket.SocketData
import org.dweb_browser.js_common.state_compose.ComposeFlow

/**
 * 把服务器端的操作同步给客户端
 */
suspend fun ComposeFlow.StateComposeFlow<*, *, *>.autoSyncOperationToClient(
    viewModelSocket: ViewModelSocket
): Job {

    val deferred = CompletableDeferred<Job>()
    CoroutineScope(Dispatchers.Default).launch {
        val job = operationFlowCore.collectServerString {
            val socketData = SocketData(
                composeFlowId = id, data = it
            )
            val jsonStr = Json.encodeToString(socketData)
            console.log("同步给Client的操作", jsonStr)
            viewModelSocket.write(jsonStr)
        }
        deferred.complete(job)
    }
    return deferred.await()
}

/**
 * 把服务器端的操作同步给客户端
 */
suspend fun ComposeFlow.ListComposeFlow<*, *, *>.autoSyncOperationToClient(
    viewModelSocket: ViewModelSocket
): Job {
    val deferred = CompletableDeferred<Job>()
    CoroutineScope(Dispatchers.Default).launch {
        val job = operationFlowCore.collectServerString {
            val socketData = SocketData(
                composeFlowId = id, data = it
            )
            val jsonStr = Json.encodeToString(socketData)
            console.log("同步给Client的操作", jsonStr)
            viewModelSocket.write(jsonStr)
        }
        deferred.complete(job)
    }
    return deferred.await()
}