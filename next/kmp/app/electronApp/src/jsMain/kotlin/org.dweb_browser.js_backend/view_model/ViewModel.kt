package org.dweb_browser.js_backend.view_model

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import node.buffer.Buffer
import node.http.IncomingMessage
import node.net.Socket
import org.dweb_browser.js_backend.ws.WS
import org.dweb_browser.js_common.network.socket.SocketData
import org.dweb_browser.js_common.network.socket.SyncState
import org.dweb_browser.js_common.state_compose.ComposeFlow
import org.dweb_browser.js_common.view_model.DataState
import org.dweb_browser.js_common.view_model.DataStateValue



/**
 *
 * 定义一个基础的ViewModel
 * 通过ViewModel自动实现同客户端ViewModel数据的同步
 *
 * 抽象功能设计
 * - 可以设置状态
 *      - 包括设置状态和同步状态给客户端
 * - 可以监听状态的变化
 *      - 监听客户端同步过来的状态
 */
open class ViewModel(
    val subDomain: String,
    val dataState: DataState
) {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    private val viewModelState: DataState = dataState
    private val sockets = mutableListOf<ViewModelSocket>()
    // 通过在执行 getViewModelSocket 解决一对多的问题

    fun _init(){
        dataState.forEach {(key, value)->
            fun syncToClient(str: String){
                val socketData = SocketData(
                    id = subDomain,
                    path = key,
                    data = str
                )
                val jsonStr = Json.encodeToString(socketData)
                syncDataToUI(jsonStr)
            }

            when(value){
                is DataStateValue.StateValue<*, *> -> {
                    value.value.operationFlowCore.collectServerString{syncToClient(it)}
                }
                is DataStateValue.ListValue<*, *> -> {
                    value.value.operationFlowCore.collectServerString{syncToClient(it)}
                }
//                is DataStateValue.MapValue -> {
//                    // TODO:
//                    throw Exception("""还没有处理""")
//                }
                else -> {
                    throw Exception("""还没有处理""")
                }
            }
        }
    }

    fun syncBySocketData(socketData: SocketData){
        // TODO: 还需要考虑嵌套？？
        val dataStateValue = dataState[socketData.path]
        console.log("dataStateValue: ", dataStateValue)
        when(dataStateValue){
            is DataStateValue.StateValue<*, *> -> {
                CoroutineScope(Dispatchers.Default).launch {
                    val operationValueContainer = dataStateValue.value.operationFlowCore.serialization.decodeFromString(socketData.data)
                    // TODO: 这里需要重新调整 临时使用emitByClient2
                    (dataStateValue.value as ComposeFlow.StateComposeFlow<Any, Any>).emitByClient(operationValueContainer.value, operationValueContainer.emitType)
                }

            }
            is DataStateValue.ListValue<*, *> -> {
                CoroutineScope(Dispatchers.Default).launch {
                    val operationValueContainer = dataStateValue.value.operationFlowCore.serialization.decodeFromString(socketData.data)
                    (dataStateValue.value as ComposeFlow.ListComposeFlow<Any, Any>).emitByClient(operationValueContainer.value, operationValueContainer.emitType)
                }
            }
//            is DataStateValue.MapValue -> {
//                // TODO:
//                throw Exception("""还没有处理 at syncBySocketData""")
//            }
            else -> {
                throw Exception("""不应该出现的类型 还没有处理 at syncBySocketData""")
            }
        }
    }



    init {
        console.log("BaseViewModel init")
        var remove = WS.onUpgrade { req: IncomingMessage, socket: Socket, head: Buffer ->
            val currentSubDomain = req.headers.host?.split(".localhost")?.get(0)?: console.error(
                """
                    moduleId == null
                    req.headers.host? : ${req.headers.host}
                    at class WS
                    at Ws.kt
                """.trimIndent()
            )

            console.log("currentSubDomain: ", currentSubDomain)

            if(currentSubDomain == subDomain){
                console.log("给 $subDomain 模块创建了socket")
                ViewModelSocket(
                    socket,
                    req.headers["sec-websocket-key"].toString(),
                ).apply {
                    onData { socketData ->
                        console.log("接受到了client onData", socketData)
                        if(socketData.id == subDomain)syncBySocketData(socketData)
                    }
                    sockets.add(this)
                    onClose { console.log("删除了 ViewModelSocket");sockets.remove(this) }
                    val that = this
                    CoroutineScope(Dispatchers.Default).launch {
                        write(SyncState.SERVER_TO_CLIENT_START.value)
                        this@ViewModel.syncDataStateToUI(null, that).await()
                        write(SyncState.SERVER_TO_CLIENT_DONE.value)
                    }
                }
            }
        }

//        // 以服务器角色更新了viewModelState之后就必须要报数据同步给UI
//        viewModelState.onUpdate(ViewModelStateRole.SERVER) { key, value, syncType ->
//            syncDataToUI(key, value, syncType)
//        }

        _init()
    }

    /**
     * 添加一个监听客户端角色更新状态的监听器
     */
//    fun onUpdateByClient(cb: OnUpdateCallback) =
//        viewModelState.onUpdate(ViewModelStateRole.CLIENT, cb)

    /**
     * 设置状态的值快捷方式
     */
//    operator fun set(key: dynamic, value: dynamic) {
//        viewModelState[key] = value
//    }

    /**
     * 第一次同同步数据给UI
     * - 之后是同步操作
     */
    private fun syncDataStateToUI(prePath: String?, viewModelSocket: ViewModelSocket): CompletableDeferred<Unit> {
        console.log("嗲用了 syncDataStateToUI： prePath: ", prePath)

        val deferred = CompletableDeferred<Unit>()
        val countTarget = dataState.size
        var countActual  = 0
        fun sync(str: String?, key: String){
            if(str != null){
                val socketData = SocketData(
                    id = subDomain,
                    path = if(prePath == null) key else "${prePath}/$key",
                    data = str
                )
                val jsonStr = Json.encodeToString(socketData)
                viewModelSocket.write(jsonStr)
                countActual++
                if(countActual == countTarget){
                    deferred.complete(Unit)
                }
            }
        }

        dataState.forEach {(key, value)->
            when(value){
                is DataStateValue.StateValue<*, *> -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        val operationValueContainer = value.value.packagingCurrentStateOperationValueContainerString()
                        sync(operationValueContainer, key)
                    }
                }
                is DataStateValue.ListValue<*, *> -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        val operationValueContainer = value.value.packagingCurrentStateOperationValueContainerString()
                        sync(operationValueContainer, key)
                    }
                }
                else -> {
                    throw Exception(""" 
                        还有没处理的类型
                        at ViewModel.kt
                    """.trimIndent())
                }
            }
        }
        return deferred
    }

    /**
     * 向UI同步数据
     * @param key {dynamic}
     * - 同步数据的key
     * @param value {dynamic
     * - 同步数据的value
     */
    private fun syncDataToUI(str: String) {
        scope.launch {
            sockets.forEach {
                it.write(str)
            }
        }
    }
}

