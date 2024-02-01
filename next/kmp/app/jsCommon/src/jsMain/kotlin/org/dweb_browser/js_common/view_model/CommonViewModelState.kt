package org.dweb_browser.js_common.view_model


typealias OnUpdateCallback = (key: String, value: dynamic, syncType: SyncType) -> Unit
typealias ViewModelMutableMap  = MutableMap<String, dynamic>

/**
 * 创建一个 ViewModelMutableMap 实例的方法
 */
fun viewModelMutableMapOf(vararg pairs: Pair<String, dynamic>) = mutableMapOf(*pairs)

/**
 * - 概述：
 * CommonViewModelState是通用的用来通ViewModel搭配使用的state，
 * 协调Server和Client的数据同步
 *
 * - 抽象功能设计
 * 1.根据 key value 保存数据
 * 2.只能够有两个角色能够更新状态
 * 3.提供监听状态变化的能力
 */
open class CommonViewModelState(
    initState: ViewModelMutableMap
){
    protected var state: ViewModelMutableMap
    private val onUpdateCallbackList = mapOf(
        ViewModelStateRole.SERVER to mutableListOf<OnUpdateCallback>(),
        ViewModelStateRole.CLIENT to mutableListOf<OnUpdateCallback>()
    )
    init {
        state = initState
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
    fun set(key: String, value:dynamic, role: ViewModelStateRole, syncType: SyncType){
        console.log("value",value)
        console.log("State: ", state)

        when(syncType.value){
            SyncType.REPLACE.value ->  state[key] = value
            SyncType.ADD.value -> {
                val v = state[key]
                require(v is List<*>)
                v.add(value)
                console.log("state[key]: ",state[key])
            }
            else -> console.error("viewModelState 还没有处理 ${syncType.value} 类型的数据设置")
        }

        onUpdateCallbackList[role]?.forEach { cb -> cb(key,value, syncType) }
    }

    /**
     * 添加状态变化监听器
     * @param cb {OnUpdateCallback}
     * - 监听器
     * @param role {ViewModelStateRole}
     * - 选择监听的角色
     */
    fun onUpdate(role: ViewModelStateRole, cb: OnUpdateCallback): () -> Unit{
        onUpdateCallbackList[role]?.add(cb)
        return {onUpdateCallbackList[role]?.remove(cb)}
    }

    /**
     * 遍历状态
     */
    fun forEach(cb: (key: dynamic, value: dynamic) -> Unit){
        state.forEach {
            cb(it.key, it.value)
        }
    }

    /**
     * 获取某一个状态的值
     */
    operator fun get(key: dynamic): dynamic{
        return state[key]
    }
}

sealed class ViewModelStateRole private constructor(val value: String){
    class Server (): ViewModelStateRole("SERVER")
    class Client (): ViewModelStateRole("CLIENT")
    companion object{
        val SERVER = Server()
        val CLIENT = Client()
    }
}