package org.dweb_browser.js_backend.view_model

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import node.buffer.Buffer
import node.http.IncomingMessage
import node.net.Socket
import node.net.SocketEvent
import node.stream.Duplex
import org.dweb_browser.js_backend.view_model_state.OnUpdateCallback
import org.dweb_browser.js_backend.view_model_state.ViewModelMutableMap
import org.dweb_browser.js_backend.view_model_state.ViewModelState
import org.dweb_browser.js_backend.view_model_state.ViewModelStateRole
import org.dweb_browser.js_backend.view_model_state.viewModelMutableMapOf
import org.dweb_browser.js_backend.ws.WS

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
open class BaseViewModel(
    val frontendViewModelId: String, initVieModelMutableMap: ViewModelMutableMap? = null
) {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    private val viewModelState: ViewModelState =
        ViewModelState(initVieModelMutableMap ?: viewModelMutableMapOf())
    private val sockets = mutableListOf<ViewModelSocket>()
    // 通过在执行 getViewModelSocket 解决一对多的问题

    init {
        console.log("BaseViewModel init")
        var remove = WS.onUpgrade { req: IncomingMessage, socket: Socket, head: Buffer ->
            val currentFrontendViewModelId = req.headers.host?.split(".localhost")?.get(0)?: console.error(
                """
                    moduleId == null
                    req.headers.host? : ${req.headers.host}
                    at class WS
                    at Ws.kt
                """.trimIndent()
            )

            if(currentFrontendViewModelId == frontendViewModelId){
                console.log("给 $frontendViewModelId 模块创建了socket")
                ViewModelSocket(socket, req.headers["sec-websocket-key"] as String, frontendViewModelId).apply {
                    onData {
                        viewModelState.set(it[0], it[1], ViewModelStateRole.CLIENT)
                    }
                    sockets.add(this)
                    onClose { sockets.remove(this) }
                    this@BaseViewModel.syncViewModelStateToUI()
                }
            }
        }

        // 以服务器角色更新了viewModelState之后就必须要报数据同步给UI
        viewModelState.onUpdate(ViewModelStateRole.SERVER) { key, value ->
            syncDataToUI(key, value)
        }
    }

    /**
     * 添加一个监听客户端角色更新状态的监听器
     */
    fun onUpdateByClient(cb: OnUpdateCallback) =
        viewModelState.onUpdate(ViewModelStateRole.CLIENT, cb)

    /**
     * 设置状态的值
     */
    operator fun set(key: dynamic, value: dynamic) {
        viewModelState[key] = value
    }

    /**
     * 同步数据给UI
     */
    private fun syncViewModelStateToUI() {
        viewModelState.forEach { key, value ->
            syncDataToUI(key, value)
        }
        // 发送初始化数据同步完成的消息
        syncDataToUI("syncDataToUiState", "sync-data-to-ui-done")
        console.log("syncViewModelStateToUI done")
    }

    /**
     * 向UI同步数据
     * @param key {dynamic}
     * - 同步数据的key
     * @param value {dynamic
     * - 同步数据的value
     */
    private fun syncDataToUI(key: dynamic, value: dynamic) {
        scope.launch {
            sockets.forEach {
                it.write(JSON.stringify(arrayOf(key, value)))
            }
        }
    }
}