package org.dweb_browser.js_backend.view_model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

typealias OnUpdateCallback = (key: dynamic, value: dynamic) -> Unit
typealias ViewModelMutableMap  = MutableMap<dynamic, dynamic>

/**
 * 创建一个 ViewModelMutableMap 实例的方法
 */
fun viewModelMutableMapOf(vararg pairs: Pair<dynamic, dynamic>) = mutableMapOf(*pairs)

/**
 * - 概述：
 * ViewModelState是用来通ViewModel搭配使用的state，
 * 协调Server和Client的数据同步
 *
 * - 抽象功能设计
 * 1.根据 key value 保存数据
 * 2.只能够有两个角色能够更新状态
 * 3.提供监听状态变化的能力
 */
class ViewModelState(
    initState: ViewModelMutableMap? = null
){
    private val role: ViewModelStateRole = ViewModelStateRole.SERVER
    private val scope = CoroutineScope(Dispatchers.Default)
    private var state: ViewModelMutableMap
    private val onUpdateCallbackList = mapOf(
        ViewModelStateRole.SERVER to mutableListOf<OnUpdateCallback>(),
        ViewModelStateRole.CLIENT to mutableListOf<OnUpdateCallback>()
    )
    init {
        state = initState?: viewModelMutableMapOf()
    }

    /**
     * 设置状态的方法
     * @param key {dynamic}
     * - 对应 map.key
     * @param value {dynamic}
     * - 对应 map.value
     * @param role {ViewModelStateRole}
     * - 更新状态的角色
     * - ViewModelStateRole.SERVER 服务器角色
     * - ViewModelStateRole.CLIENT 客户端角色
     */
    fun set(key: dynamic, value:dynamic, role: ViewModelStateRole){
        state[key] = value
        onUpdateCallbackList[role]?.forEach { cb -> cb(key,value) }
    }

    /**
     * - 为服务器角色添加一个更新状态的快捷方式
     * - viewModelState[key] = value
     */
    operator fun set(key: dynamic, value:dynamic){
        state[key] = value
        onUpdateCallbackList[ViewModelStateRole.SERVER]?.forEach {
            it(key, value)
        }
    }

    /**
     * 添加状态变化监听器
     * @param cb {OnUpdateCallback}
     * - 监听器
     * @param role {ViewModelStateRole}
     * - 选择监听的角色
     */
    fun onUpdate(role: ViewModelStateRole, cb: OnUpdateCallback, ): () -> Unit{
        onUpdateCallbackList[role]?.add(cb)
        return {onUpdateCallbackList[role]?.remove(cb)}
    }

    /**
     * 遍历状态
     */
    fun forEach(cb: OnUpdateCallback){
        state.forEach {
            cb(it.key, it.value)
        }
    }

    operator fun get(key: dynamic): dynamic{
        return state[key]
    }
}

sealed class ViewModelStateRole private constructor(val Value: String){
    class Server (): ViewModelStateRole("SERVER")
    class Client (): ViewModelStateRole("CLIENT")
    companion object{
        val SERVER = Server()
        val CLIENT = Client()
    }
}

