package org.dweb_browser.js_frontend.view_model

import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.js_common.view_model.SyncType
import org.dweb_browser.js_common.view_model.ViewModelStateRole
import org.dweb_browser.js_common.view_model.EncodeValueToString
import org.dweb_browser.js_common.view_model.DecodeValueFromString

typealias HandleMessageDataList = (key: String, value: dynamic, syncType: SyncType) -> Unit


open class ViewModel(
    val state: ViewModelState,
    val encodeValueToString: EncodeValueToString,/**编码value的方法*/
    val decodeValueFromString: DecodeValueFromString, /**解码value的方法*/
) {
    val viewModelSocket = ViewModelSocket("ws://${window.location.host}")
    private val handleMessageDataList = mutableListOf<HandleMessageDataList>()

    val whenSyncDataFromServerDone = CompletableDeferred<Unit>()
    init {
        viewModelSocket.onSyncFromServer {key, valueString, syncType ->
            val value = when(key){
                "syncDataToUiState" -> valueString
                else -> decodeValueFromString(key, valueString, syncType)
            }
            handleMessageDataList.forEach { cb -> cb(key, value, syncType) }
        }

        handleMessageDataList.add() {key, value, syncType ->
            when{
                key == "syncDataToUiState" && value == "sync-data-to-ui-done" ->{
                    if(!whenSyncDataFromServerDone.isCompleted)whenSyncDataFromServerDone.complete(Unit)
                }
                else -> state.set(key, value, ViewModelStateRole.SERVER, syncType)
            }
        }
        state.onUpdate(ViewModelStateRole.CLIENT, ::syncStateToServer)
        viewModelSocket.start()
    }

    /**
     * 同步数据到 Server
     */
    private fun syncStateToServer(key: String, value: dynamic, syncType: SyncType){
        console.log("syncStateToServer", key, value)
        val valueString = encodeValueToString(key, value, syncType)
        console.log("valueString: ", valueString)
        viewModelSocket.syncToServer(key, valueString, syncType)
    }
}



