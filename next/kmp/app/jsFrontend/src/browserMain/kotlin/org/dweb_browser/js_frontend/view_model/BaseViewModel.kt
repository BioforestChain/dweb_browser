package org.dweb_browser.js_frontend.view_model

import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred

typealias HandleMessageDataList = (key: String, value: dynamic) -> Unit
typealias EncodeValueToString = (key: String, value: dynamic) -> String
typealias DecodeValueFromString = (key: String, value: String) -> dynamic

open class ViewModel(
    val state: ViewModelState,
    val encodeValueToString: EncodeValueToString,/**编码value的方法*/
    val decodeValueFromString: DecodeValueFromString, /**解码value的方法*/
) {
    val viewModelSocket = ViewModelSocket("ws://${window.location.host}")
    private val handleMessageDataList = mutableListOf<HandleMessageDataList>()

    val whenSyncDataFromServerDone = CompletableDeferred<Unit>()
    init {
        viewModelSocket.onSyncFromServer {key, valueString ->
            val value = when(key){
                "syncDataToUiState" -> valueString
                else -> decodeValueFromString(key, valueString)
            }
            handleMessageDataList.forEach { cb -> cb(key, value) }
        }

        handleMessageDataList.add() {key, value ->

            when{
                key == "syncDataToUiState" && value == "sync-data-to-ui-done" ->{
                    if(!whenSyncDataFromServerDone.isCompleted)whenSyncDataFromServerDone.complete(Unit)
                }
                else -> state[key] = value
            }
        }
        state.onUpdate(::syncStateToServer)
        viewModelSocket.start()
    }

    /**
     * 同步数据到 Server
     */
    private fun syncStateToServer(key: String, value: dynamic){
        val valueString = encodeValueToString(key, value)
        viewModelSocket.syncToServer(key, valueString)
    }
}



