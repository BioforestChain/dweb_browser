package org.dweb_browser.js_frontend.view_model

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.js_common.network.socket.SocketData
import org.dweb_browser.js_common.view_model.DataState
import org.dweb_browser.js_common.view_model.DataStateValue
import org.dweb_browser.js_frontend.network.socket.Socket

typealias OnSyncFromServer = DataState.(socketData: SocketData) -> Unit
class ViewModelDataState(
    val dataState: DataState,
    val VMId: String,
    val onSyncFromServer: OnSyncFromServer
) {
    val socket = Socket("ws://${window.location.host}")
    init {
        socket.onSyncFromServer {str: String->
            val socketData = Json.decodeFromString<SocketData>(str)
            if(socketData.id == VMId) onSyncFromServer(dataState,socketData)
        }

        dataState.forEach {(path,value) ->
            fun syncToServer(str: String){
                val socketData = SocketData(
                    id = VMId,
                    path = path,
                    data = str
                )
                val jsonStr = Json.encodeToString(socketData)
                console.log(path, jsonStr)
                socket.syncToServer(jsonStr)
            }

            when(value){
                is DataStateValue.StateValue<*, *> -> {
                    value.value.operationFlowCore.collectClientString{
                        syncToServer(it)
                    }
                }
                is DataStateValue.ListValue<*, *> -> {
                    value.value.operationFlowCore.collectClientString{
                        syncToServer(it)
                    }
                }
//                is DataStateValue.MapValue -> {
//                    // TODO:
//                    throw Exception("""
//                        还没有处理
//                        at ViewModelDataState.kt
//
//                       """.trimIndent())
//                }
                else -> {
                    throw Exception("""
                        还没有处理
                        at ViewModelDataState.kt
                        """.trimIndent())
                }
            }

        }

        socket.start()
    }
}



