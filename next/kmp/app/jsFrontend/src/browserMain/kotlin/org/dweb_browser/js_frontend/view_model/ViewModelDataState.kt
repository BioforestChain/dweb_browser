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

    val socket = Socket("ws://${window.location.host}")
    val whenSyncDataStateStart = CompletableDeferred<Unit>()
    val whenSyncDataStateDone = CompletableDeferred<Unit>()

    fun add(composeFlow: ComposeFlow<*, *, *>): () -> Unit{
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
                    true
                }
                SyncState.SERVER_TO_CLIENT_DONE.value -> {
                    whenSyncDataStateDone.complete(Unit)
                    true
                }
                else -> false
            }
        }
        socket.start()
    }
}



