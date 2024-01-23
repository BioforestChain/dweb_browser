package org.dweb_browser.js_frontend.view_model

import js.json.JSON
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.dweb_browser.js_frontend.dweb_web_socket.DwebWebSocket
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import org.dweb_browser.js_frontend.ViewModel
import react.StateSetter
import react.useState
import kotlin.reflect.KProperty


typealias HandleMessageDataList = (arr: dynamic) -> Unit


abstract class BaseViewModel(
    frontendViewModelId: String,
) : DwebWebSocket("ws://${window.location.host}?frontend_view_module_id=$frontendViewModelId") {
    abstract var state: MutableMap<dynamic, dynamic>

    private val handleMessageDataList = mutableListOf<HandleMessageDataList>()

    /**
     *
     */
    val whenSyncDataFromServerStart = CompletableDeferred<Unit>()
    init {
        onMessage {
            val data = it.data
            require(data is String)
            val arr: dynamic = JSON.parse(data)
            handleMessageDataList.forEach { cb -> cb(arr) }
        }

        handleMessageDataList.add() {
            // 同步状态
            state[it[0] as String] = it[1] as Any
            if(!whenSyncDataFromServerStart.isCompleted)whenSyncDataFromServerStart.complete(Unit)
        }
    }

    // TODO: toFlow 必须只能够返回一个
    fun toFlow() = channelFlow<Array<String>> {
        fun handle(it: dynamic) {
            scope.launch {
                this@channelFlow.send(it)
            }
        }
        handleMessageDataList.add(::handle)
        awaitClose {
            handleMessageDataList.remove(::handle)
        }
    }

    /**
     * 设置状态的值
     */
    fun <ValueType> set(key: String, value: ValueType){
        state[key] = value
        val a = arrayOf(key, value)
        val str = kotlin.js.JSON.stringify(a)
        send(str)
    }

    /**
     * 把当前BaseViewModel转为ReactState使用
     */
    fun <T> toUseState(key: dynamic): RectState<T>{
        return RectState<T>(state[key] as T, key, this@BaseViewModel)
    }

    // TODO: 这里需要删除 - 需要整理代码
    class RectState<T>(arg: T, private val key: dynamic, val viewModel: BaseViewModel){
        private var state: dynamic = null
        private var setState: dynamic = null

        init {
            val (_state, _setState) = useState<dynamic>(arg)
            state = _state
            setState = _setState
            CoroutineScope(Dispatchers.Default).launch {
                viewModel.toFlow().collect{
                    if(it[0] == key){
                        setState(it[1])
                    }
                }
            }
        }
//        operator fun component1(): T = state.component1()
//        operator fun component2(): StateSetter<T> = state.component2()
        //        operator fun getValue(
//            thisRef: Nothing?,
//            property: KProperty<*>,
//        ): T= state.getValue(thisRef, property)

        //        operator fun getValue(
//            thisRef: Nothing?,
//            property: KProperty<*>,
//        ): T= _value
//        operator fun setValue(
//            thisRef: Nothing?,
//            property: KProperty<*>,
//            value: T,
//        ) {
//            // 设置ViewModel的数据
//            viewModel.set(key, value)
//            console.log(thisRef, property, value)
//            state.setValue(thisRef, property, value)
//            _value = value
//        }
        operator fun component1(): T = state

        operator fun component2(): StateSetter<T> = setState

        operator fun getValue(
            thisRef: Nothing?,
            property: KProperty<*>,
        ): T= state
        operator fun setValue(
            thisRef: Nothing?,
            property: KProperty<*>,
            value: T,
        ) {
            // 设置ViewModel的数据
            viewModel.set(key, value)
            setState(value)
        }
    }
}

