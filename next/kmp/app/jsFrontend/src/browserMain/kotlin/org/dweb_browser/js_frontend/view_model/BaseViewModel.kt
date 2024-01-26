package org.dweb_browser.js_frontend.view_model

import androidx.compose.runtime.Composable
import js.json.JSON
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import org.dweb_browser.js_frontend.dweb_web_socket.DwebWebSocket
import react.StateSetter
import react.useState
import kotlin.reflect.KProperty
import androidx.compose.runtime.MutableState as IMutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.collect
import org.dweb_browser.js_frontend.view_model_state.ViewModelState


typealias HandleMessageDataList = (arr: dynamic) -> Unit


abstract class BaseViewModel(
    frontendViewModelId: String,
    val state: ViewModelState
) {
    val dwebWebSocket = DwebWebSocket("ws://${window.location.host}?frontend_view_module_id=$frontendViewModelId")
    private val handleMessageDataList = mutableListOf<HandleMessageDataList>()

    /**
     *
     */
    val whenSyncDataFromServerDone = CompletableDeferred<Unit>()
    init {
        dwebWebSocket.onMessage {
            val data = it.data
            require(data is String)
            val arr: dynamic = JSON.parse(data)
            handleMessageDataList.forEach { cb -> cb(arr) }
        }

        handleMessageDataList.add() {
            val key = it[0]
            val value = it[1]
            console.log("接受到了从服务器端同步过来的数据： ", key, value)
            when{
                key is String && key == "syncDataToUiState" && value == "sync-data-to-ui-done" ->{
                    if(!whenSyncDataFromServerDone.isCompleted)whenSyncDataFromServerDone.complete(Unit)
                }
                else -> state[it[0] as String] = it[1] as Any
            }
        }
        state.onUpdate(::syncStateToServer)
    }

    /**
     * 同步数据到 Server
     */
    fun syncStateToServer(key: dynamic, value: dynamic){
        val a = arrayOf(key, value)
        val str = kotlin.js.JSON.stringify(a)
        dwebWebSocket.send(str)
    }
}

