package org.dweb_browser.js_frontend.state_compose

import kotlinx.coroutines.CompletableDeferred
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
 * 自动同步操作给服务端
 */
suspend fun ComposeFlow.StateComposeFlow<*,*,*>.autoSyncOperationToServer(
    socket: Socket
): Job{
    val deferred = CompletableDeferred<Job>()
    CoroutineScope(Dispatchers.Default).launch {
        val job = operationFlowCore.collectClientString{
            val socketData = SocketData(
                composeFlowId = id,
                data = it
            )
            val jsonStr = Json.encodeToString(socketData)
            socket.syncToServer(jsonStr)
        }
        deferred.complete(job)
    }
    return deferred.await()
}

/**
 * 自动同步操作给服务端
 */
suspend fun ComposeFlow.ListComposeFlow<*,*,*>.autoSyncOperationToServer(
    socket: Socket
): Job{
    val deferred = CompletableDeferred<Job>()
    CoroutineScope(Dispatchers.Default).launch {
        val job = operationFlowCore.collectClientString{
            val socketData = SocketData(
                composeFlowId = id,
                data = it
            )
            val jsonStr = Json.encodeToString(socketData)
            socket.syncToServer(jsonStr)
        }
        deferred.complete(job)
    }
    return deferred.await()
}





