package org.dweb_browser.js_backend.view_model

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dweb_browser.js_backend.view_model_state.OnUpdateCallback
import org.dweb_browser.js_backend.view_model_state.ViewModelMutableMap
import org.dweb_browser.js_backend.view_model_state.ViewModelState
import org.dweb_browser.js_backend.view_model_state.ViewModelStateRole
import org.dweb_browser.js_backend.view_model_state.viewModelMutableMapOf

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
abstract class BaseViewModel(
    val frontendViewModelId: String,
    initVieModelMutableMap: ViewModelMutableMap? = null
){
    var whenReady = CompletableDeferred<Unit>()
    val scope = CoroutineScope(Dispatchers.Unconfined)
    private val viewModelState: ViewModelState = ViewModelState(initVieModelMutableMap?:viewModelMutableMapOf())
    private var socket: CompletableDeferred<ViewModelSocket> = ViewModelSocket.getViewModelSocket(frontendViewModelId)

    init {
        scope.launch {
            socket.await().run {
                onData {
                    // 接受到了UI同步过来的数据
                    viewModelState.set(it[0], it[1], ViewModelStateRole.CLIENT)
                }
                syncViewModelStateToUI()
                whenReady.complete(Unit)
            }
        }

        // 以服务器角色更新了viewModelState之后就必须要报数据同步给UI
        viewModelState.onUpdate(ViewModelStateRole.SERVER){key, value ->
            syncDataToUI(key,value)
        }
    }

    /**
     * 添加一个监听客户端角色更新状态的监听器
     */
    fun onUpdateByClient(cb: OnUpdateCallback) = viewModelState.onUpdate(ViewModelStateRole.CLIENT, cb)

    /**
     * 设置状态的值
     */
    operator fun set(key: dynamic, value: dynamic){
        viewModelState[key] = value
    }

    /**
     * 同步数据给UI
     */
    private suspend fun syncViewModelStateToUI(){
        socket.await().run {
            viewModelState.forEach {key, value ->
                syncDataToUI(key, value)
            }
            // 发送初始化数据同步完成的消息
            syncDataToUI("syncDataToUiState", "sync-data-to-ui-done")
        }
    }

    /**
     * 向UI同步数据
     * @param key {dynamic}
     * - 同步数据的key
     * @param value {dynamic
     * - 同步数据的value
     */
    private fun syncDataToUI(key: dynamic, value: dynamic){
        scope.launch {
            socket.await().run {
                write(JSON.stringify(arrayOf(key, value)))
            }
        }
    }
}