package org.dweb_browser.js_backend.view_model

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 *
 * 定义一个基础的ViewModel
 * @property state {MutableMap<dynamic, dynamic>}
 * - 需要再继承类中实现的抽象类
 * @property scope {CoroutineScope}
 * @property whenReady {CompletableDeferred<Unit>}
 * - 是否准备好发送数据的表示
 * @property onStateChangeByBrowser {(cb: OnDataCallback) -> Unit}
 * - 添加监听状态的变化监听器
 * @property syncDataToUI {(key: dynamic, value: dynamic) -> Unit}
 * - 同步数据给UI的方法
 *
 * example
 * class BrowserViewModel(frontendViewModelId: String) : BaseViewModel(frontendViewModelId) {
 *     // 测试数据
 *     override val state = mutableMapOf<dynamic, dynamic>("currentCount" to 1)
 *     init {
 *         // 添加一个状态监听器
 *         onStateChangeByBrowser {
 *             // 当接受到UI同步过来的数据会调用
 *             // 不需要再这里进行状态同步的操作BaseViewModel会自动更新State
 *             scope.launch {
 *                 // 把服务端的State同步到UI的viewModel
 *                 syncDataToUI(it[0], it[1] + 1)
 *             }
 *         }
 *     }
 * }
 *
 */
abstract class BaseViewModel(
    val frontendViewModelId: String
){
    abstract val state: MutableMap<dynamic, dynamic>
    val scope = CoroutineScope(Dispatchers.Unconfined)
    var whenReady = CompletableDeferred<Unit>()
    private var socket: CompletableDeferred<ViewModelSocket> = ViewModelSocket.getViewModelSocket(frontendViewModelId)

    init {
        scope.launch {
            socket.await().run {
                onData {
                    console.log("服务端 同步状态的数据 it", it)
                    // 用来同步状态
                    // TODO: 需要添加一个判断有 
                    state[it[0]] = it[1]
                }
                syncStateToUI()
                whenReady.complete(Unit)
            }
        }
    }

    /**
     * 添加状态变化的监听器方法
     */
    fun onStateChangeByBrowser(cb: OnDataCallback){
        scope.launch {
            socket.await().run {
                onData(cb)
            }
        }
    }
    /**
     * 同步数据给UI
     */
    private suspend fun syncStateToUI(){
        socket.await().run {
            state.forEach {
                syncDataToUI(it.key, it.value)
            }
        }
    }

    /**
     * 向UI同步数据
     * @param key {dynamic}
     * - 同步数据的key
     * @param value {dynamic
     * - 同步数据的value
     */
    fun syncDataToUI(key: dynamic, value: dynamic){
        scope.launch {
            socket.await().run {
                write(JSON.stringify(arrayOf(key, value)))
            }
        }
    }
}