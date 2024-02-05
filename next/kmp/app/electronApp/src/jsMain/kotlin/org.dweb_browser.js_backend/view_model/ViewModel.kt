package org.dweb_browser.js_backend.view_model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import node.buffer.Buffer
import node.http.IncomingMessage
import node.net.Socket
import org.dweb_browser.js_backend.ws.WS
import org.dweb_browser.js_common.view_model.SyncType
import org.dweb_browser.js_common.view_model.ViewModelMutableMap
import org.dweb_browser.js_common.view_model.ViewModelStateRole
import org.dweb_browser.js_common.view_model.OnUpdateCallback
import org.dweb_browser.js_common.view_model.EncodeValueToString
import org.dweb_browser.js_common.view_model.DecodeValueFromString



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
    val encodeValueToString: EncodeValueToString,
    val decodeValueFromString: DecodeValueFromString,
    initVieModelMutableMap: ViewModelMutableMap
) {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    private val viewModelState: ViewModelState =
        ViewModelState(initVieModelMutableMap)
    private val sockets = mutableListOf<ViewModelSocket>()
    // 通过在执行 getViewModelSocket 解决一对多的问题

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

            if(currentSubDomain == subDomain){
                console.log("给 $subDomain 模块创建了socket")
                ViewModelSocket(
                    socket, 
                    req.headers["sec-websocket-key"].toString(), 
                ).apply {
                    onData { key: String, value: String, syncType: SyncType ->
                        val v = decodeValueFromString(key, value, syncType)
                        viewModelState.set(key, v, ViewModelStateRole.CLIENT, syncType)
                    }
                    sockets.add(this)
                    onClose { console.log("删除了 ViewModelSocket");sockets.remove(this) }
                    this@ViewModel.syncViewModelStateToUI()
                }
            }
        }

        // 以服务器角色更新了viewModelState之后就必须要报数据同步给UI
        viewModelState.onUpdate(ViewModelStateRole.SERVER) { key, value, syncType ->
            syncDataToUI(key, value, syncType)
        }
    }

    /**
     * 添加一个监听客户端角色更新状态的监听器
     */
    fun onUpdateByClient(cb: OnUpdateCallback) =
        viewModelState.onUpdate(ViewModelStateRole.CLIENT, cb)

    /**
     * 设置状态的值快捷方式
     */
    operator fun set(key: dynamic, value: dynamic) {
        viewModelState[key] = value
    }

    /**
     * 同步数据给UI
     */
    private fun syncViewModelStateToUI() {
        viewModelState.forEach { key, value ->
            syncDataToUI(key, value, SyncType.REPLACE)
        }
        // 发送初始化数据同步完成的消息
        syncDataToUI("syncDataToUiState", "sync-data-to-ui-done", SyncType.REPLACE)
        console.log("syncViewModelStateToUI done")
    }

    /**
     * 向UI同步数据
     * @param key {dynamic}
     * - 同步数据的key
     * @param value {dynamic
     * - 同步数据的value
     */
    private fun syncDataToUI(key: String, value: dynamic, syncType: SyncType) {
        scope.launch {
            sockets.forEach {
                val valueString = when (key) {
                    "syncDataToUiState" -> value
                    else -> encodeValueToString(key, value, syncType)
                }
                it.write(key, valueString.toString(), syncType)
            }
        }
    }
}
