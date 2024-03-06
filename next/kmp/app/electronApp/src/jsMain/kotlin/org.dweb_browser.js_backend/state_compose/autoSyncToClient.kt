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
 * 把服务器端的数据同步给客户端
 */
fun ComposeFlow.StateComposeFlow<*,*,*>.syncCurrentDataToClient(
    viewModelSocket: ViewModelSocket
){
    CoroutineScope(Dispatchers.Default).launch {
        packagingCurrentStateOperationValueContainerString()?.apply {
            val socketData = SocketData(
                composeFlowId = id,
                data = this
            )
            val jsonStr = Json.encodeToString(socketData)
            viewModelSocket.write(jsonStr)
        }
    }
}

/**
 * 通过从客户端发送过来的操作
 */
fun ComposeFlow.StateComposeFlow<*, *, *>.autoSyncOperationFromClient(
    viewModelSocket: ViewModelSocket
){
    viewModelSocket.onData {
        val socketData = Json.decodeFromString<SocketData>(it)
        console.log("socketData.composeFlowId", socketData.composeFlowId, id)
        when(socketData.composeFlowId){
            id -> CoroutineScope(Dispatchers.Default).launch {
                val operationValueContainer = operationFlowCore.serialization.decodeFromString(socketData.data)
                console.log("emitByClient", operationValueContainer.value, operationValueContainer.emitType)
                emitByClient(operationValueContainer.value, operationValueContainer.emitType)
            }
        }
    }
}

/**
 * 把服务器端的操作同步给客户端
 */
suspend fun ComposeFlow.StateComposeFlow<*, *, *>.autoSyncOperationToClient(
    viewModelSocket: ViewModelSocket
): Job {

    val deferred = CompletableDeferred<Job>()
    CoroutineScope(Dispatchers.Default).launch {
        val job = operationFlowCore.collectServerString{
            val socketData = SocketData(
                composeFlowId = id,
                data = it
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
 * 把服务器端的数据同步给客户端
 */
fun ComposeFlow.ListComposeFlow<*,*,*>.syncCurrentDataToClient(
    viewModelSocket: ViewModelSocket
){
    CoroutineScope(Dispatchers.Default).launch {
        packagingCurrentStateOperationValueContainerString()?.apply {
            val socketData = SocketData(
                composeFlowId = id,
                data = this
            )
            val jsonStr = Json.encodeToString(socketData)
            viewModelSocket.write(jsonStr)
        }
    }
}

/**
 * 通过从客户端发送过来的操作
 */
fun ComposeFlow.ListComposeFlow<*, *, *>.autoSyncOperationFromClient(
    viewModelSocket: ViewModelSocket
){
    viewModelSocket.onData {
        val socketData = Json.decodeFromString<SocketData>(it)
        when(socketData.composeFlowId){
            id -> CoroutineScope(Dispatchers.Default).launch {
                val operationValueContainer = operationFlowCore.serialization.decodeFromString(socketData.data)
                emitByClient(operationValueContainer.value, operationValueContainer.emitType)
            }
            else -> {}
        }
    }
}

/**
 * 把服务器端的操作同步给客户端
 */
suspend fun ComposeFlow.ListComposeFlow<*, *, *>.autoSyncOperationToClient(
    viewModelSocket: ViewModelSocket
): Job{
    val deferred = CompletableDeferred<Job>()
    CoroutineScope(Dispatchers.Default).launch {
        val job = operationFlowCore.collectServerString{
            val socketData = SocketData(
                composeFlowId = id,
                data = it
            )
            val jsonStr = Json.encodeToString(socketData)
            console.log("同步给Client的操作", jsonStr)
            viewModelSocket.write(jsonStr)
        }
        deferred.complete(job)
    }
    return deferred.await()
}