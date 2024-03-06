package org.dweb_browser.js_backend.view_model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import node.buffer.Buffer
import node.http.IncomingMessage
import node.net.Socket
import node.url.parse
import org.dweb_browser.js_backend.state_compose.autoSyncOperationFromClient
import org.dweb_browser.js_backend.state_compose.autoSyncOperationToClient
import org.dweb_browser.js_backend.state_compose.syncCurrentDataToClient
import org.dweb_browser.js_backend.ws.WS
import org.dweb_browser.js_common.state_compose.ComposeFlow

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

) {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    private val composeFlowList = mutableListOf<ComposeFlow<*,*,*>>()
    private val viewModelSockets = mutableListOf<ViewModelSocket>()

    private fun MutableList<ViewModelSocket>.ViewModelSocketsAdd(viewModelSocket: ViewModelSocket){
        composeFlowList.forEach {
            when(it){
                is ComposeFlow.StateComposeFlow<*,*,*> -> {
                    it.syncCurrentDataToClient(viewModelSocket)
                    it.autoSyncOperationFromClient(viewModelSocket)
                    CoroutineScope(Dispatchers.Default).launch {
                        val job = it.autoSyncOperationToClient(viewModelSocket)
                        viewModelSocket.onClose{
                            job.cancel()
                        }
                    }
                }
                is ComposeFlow.ListComposeFlow<*,*,*> -> {
                    it.syncCurrentDataToClient(viewModelSocket)
                    it.autoSyncOperationFromClient(viewModelSocket)
                    CoroutineScope(Dispatchers.Default).launch {
                        val job = it.autoSyncOperationToClient(viewModelSocket)
                        viewModelSocket.onClose{
                            job.cancel()
                        }
                    }
                }
            }
        }
    }

    init {
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
            if(currentSubDomain == subDomain && req.url == "/sync_data"){
                console.log("给 $subDomain 模块创建了socket")
                ViewModelSocket(
                    socket,
                    req.headers["sec-websocket-key"].toString(),
                ).apply {
                    viewModelSockets.ViewModelSocketsAdd(this)
                }
            }
        }
    }

    fun composeFlowListAdd(cb: ComposeFlow<*,*,*>): () -> Unit{
        composeFlowList.add(cb)

        when(cb){
            is ComposeFlow.StateComposeFlow<*,*,*> -> {
                viewModelSockets.forEach { socket ->
                    cb.syncCurrentDataToClient(socket)
                    cb.autoSyncOperationFromClient(socket)
                    CoroutineScope(Dispatchers.Default).launch {
                        val job = cb.autoSyncOperationToClient(socket)
                        socket.onClose{
                            job.cancel()
                        }
                    }
                }
            }
            is ComposeFlow.ListComposeFlow<*,*,*> -> {
                viewModelSockets.forEach { socket ->
                    cb.syncCurrentDataToClient(socket)
                    cb.autoSyncOperationFromClient(socket)
                    CoroutineScope(Dispatchers.Default).launch {
                        val job = cb.autoSyncOperationToClient(socket)
                        socket.onClose{
                            job.cancel()
                        }
                    }
                }
            }
        }

        // 原则上一旦添加是不可以删除，否则会导致客户端和移动端的数据不匹配
        return {composeFlowList.remove(cb)}
    }
}

