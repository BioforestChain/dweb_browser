package org.dweb_browser.js_frontend.view_model

import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.js_common.network.socket.SocketData
import org.dweb_browser.js_common.network.socket.SyncState
import org.dweb_browser.js_common.state_compose.ComposeFlow
import org.dweb_browser.js_frontend.network.socket.Socket
import org.dweb_browser.js_frontend.state_compose.*

class ViewModelDataState(
    val VMId: String,
) {

    val composeFlowList = mutableListOf<ComposeFlow<*, *, *>>()
    // path == /sync_data
    // 表示这socket的连接使用来做同步状态的
    val socket = Socket("ws://${window.location.host}/sync_data")
    val whenSyncDataStateStart = CompletableDeferred<Unit>()
    val whenSyncDataStateDone = CompletableDeferred<Unit>()

    fun composeFlowListAdd(composeFlow: ComposeFlow<*, *, *>): () -> Unit{
        when(composeFlow){
            is ComposeFlow.StateComposeFlow<*, *, *> -> composeFlow.autoSyncToServerStart(socket)
            is ComposeFlow.ListComposeFlow<*, *, *> -> composeFlow.autoSyncToServerStart(socket)
        }
        composeFlowList.add(composeFlow)
        return {composeFlowList.remove(composeFlow)}
    }

    init {
        socket.onMessage {
            when(it){
                SyncState.SERVER_TO_CLIENT_START.value -> {
                    whenSyncDataStateStart.complete(Unit)
                }
                SyncState.SERVER_TO_CLIENT_DONE.value -> {
                    whenSyncDataStateDone.complete(Unit)
                }
            }
        }
        socket.start()
    }
}



