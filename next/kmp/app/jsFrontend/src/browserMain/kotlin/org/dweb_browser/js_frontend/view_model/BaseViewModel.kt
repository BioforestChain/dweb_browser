package org.dweb_browser.js_frontend.view_model

import androidx.compose.runtime.Composable
import js.json.JSON
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import org.dweb_browser.js_frontend.dweb_web_socket.DwebWebSocket
import react.StateSetter
import react.useState
import kotlin.reflect.KProperty
import androidx.compose.runtime.MutableState as IMutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember


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

    // TODO: 创建 mutableState 对象
    // TODO: 保存 mutableState 对象
    // TODO: 不能够采用一对多，否则会有问题
    // TODO: 需要传递一个Key 实现转为mutableState
    // TODO: 相同的Key返回相同的MutableState
    // TODO: 生产？ 更新UI ？ 同步数据

    private val mutableStateMap = mutableMapOf<Any, MutableState<dynamic>>()
    @Composable
    fun toMutableStateOf(key: dynamic) = remember {
        val initValue = state[key]
        if(initValue == null){
            throw(Throwable("""
                state[key] == null
                key = $key
                at toMutableStateOf
            """.trimIndent()))
        }
        var mutableState = mutableStateMap[key]
        if(mutableState == null){
            mutableState = MutableState(state[key])
            mutableStateMap[key] = mutableState
        }
        mutableState
    }

    class MutableState<T>(val initValue: T) : IMutableState<T> {

        val mutableState = mutableStateOf<T>(initValue)
        override var value: T
            get() = mutableState.value
            set(value) {
                console.log("需要同步UI的数据到服务器")
                // TODO: 需要同步UI的数据到服务器
                mutableState.value = value
            }

        override fun component1() = mutableState.component1()
        override fun component2() = mutableState.component2()
        operator fun getValue(thisObj: Any?, property: KProperty<*>): T = value
        operator fun setValue(
            thisObj: Any?, property: KProperty<*>, value: T
        ) {
            this.value = value
        }
    }
}

