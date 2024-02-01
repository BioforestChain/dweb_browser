package org.dweb_browser.js_frontend.view_model

import js.json.JSON
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.js_frontend.dweb_web_socket.DwebWebSocket
import org.dweb_browser.js_frontend.view_model_state.ViewModelState
import org.jetbrains.skia.Region
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

typealias HandleMessageDataList = (arr: dynamic) -> Unit
typealias EncodeValueToString = (key: String, value: dynamic) -> String
typealias DecodeValueFromString = (key: String, value: String) -> dynamic

open class BaseViewModel(
    val state: ViewModelState,
    val encodeValueToString: EncodeValueToString,/**编码value的方法*/
    val decodeValueFromString: DecodeValueFromString,/**解码value的方法*/
) {
    val dwebWebSocket = DwebWebSocket("ws://${window.location.host}")
    private val handleMessageDataList = mutableListOf<HandleMessageDataList>()

    /**
     *
     */
    val whenSyncDataFromServerDone = CompletableDeferred<Unit>()
    init {
        dwebWebSocket.onMessage {
            val data = it.data
            require(data is String)
            val syncData = Json.decodeFromString<SyncData>(data)
            val key = syncData.key
            val value = when(key){
                "syncDataToUiState" -> syncData.value
                else -> decodeValueFromString(key, syncData.value)
            }
            handleMessageDataList.forEach { cb -> cb(arrayOf(key, value)) }
        }

        handleMessageDataList.add() {
            val key = it[0]
            val value = it[1]
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
        val valueString = encodeValueToString(key, value)
        val syncData = SyncData(key.toString(), valueString)
        val jsonSyncData = Json.encodeToString<SyncData>(syncData)
        dwebWebSocket.send(jsonSyncData)
    }
}

@Serializable
data class SyncData(
    @JsName("key")
    val key: String,
    @JsName("value")
    val value: String
)

