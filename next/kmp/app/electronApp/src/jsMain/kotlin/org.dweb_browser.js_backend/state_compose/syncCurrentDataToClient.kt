package org.dweb_browser.js_backend.state_compose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.js_backend.view_model.ViewModelSocket
import org.dweb_browser.js_common.network.socket.SocketData
import org.dweb_browser.js_common.state_compose.ComposeFlow

/**
 * 把服务器端的数据同步给客户端
 */
fun ComposeFlow.StateComposeFlow<*, *, *>.syncCurrentDataToClient(
    viewModelSocket: ViewModelSocket
) {
    CoroutineScope(Dispatchers.Default).launch {
        packagingCurrentStateOperationValueContainerString()?.apply {
            val socketData = SocketData(
                composeFlowId = id, data = this
            )
            val jsonStr = Json.encodeToString(socketData)
            viewModelSocket.write(jsonStr)
        }
    }
}

/**
 * 把服务器端的数据同步给客户端
 */
fun ComposeFlow.ListComposeFlow<*, *, *>.syncCurrentDataToClient(
    viewModelSocket: ViewModelSocket
) {
    CoroutineScope(Dispatchers.Default).launch {
        packagingCurrentStateOperationValueContainerString()?.apply {
            val socketData = SocketData(
                composeFlowId = id, data = this
            )
            val jsonStr = Json.encodeToString(socketData)
            viewModelSocket.write(jsonStr)
        }
    }
}