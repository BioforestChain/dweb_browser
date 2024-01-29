package org.dweb_browser.js_frontend.view_model_state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import react.StateSetter
import react.useState
import kotlin.reflect.KProperty


typealias OnUpdateCallback = (key: dynamic, value: dynamic) -> Unit
typealias ViewModelMutableMap  = MutableMap<dynamic, dynamic>
typealias ViewModelStateFlowContextType = Array<dynamic>
fun viewModelMutableMapOf(vararg pairs: Pair<dynamic, dynamic>) = mutableMapOf(*pairs)
class ViewModelState(
    initState: ViewModelMutableMap? = null
){

    private val scope = CoroutineScope(Dispatchers.Default)
    private val mutableSharedFlow = MutableSharedFlow<ViewModelStateFlowContextType>()

    private var _state: ViewModelMutableMap
    init {
        _state = initState?: viewModelMutableMapOf()
    }

    /**
     * 设置 _state 状态的时候使用
     * @param key {dynamic}
     * - 对应 map.key
     * @param value {dynamic}
     * - 对应 map.value
     * @param isInside {Boolean}
     * - 是否是内部设置
     * - true 表示是内部设置会执行通过 onUpdate 添加的监听器
     * - false 表示是外部设置，会同步给 useState mutableStateOf...
     */
    fun set(key: dynamic, value:dynamic, isInside: Boolean = false){
        _state.put(key, value)
        if(isInside){
            _onUpdateCallbackList.forEach { cb -> cb(key, value) }
        }else{
            scope.launch {
                // 同步给 toUseState 对象 toMutableState 对象
                mutableSharedFlow.emit(arrayOf(key, value))
            }
        }
    }

    operator fun set(key: dynamic, value:dynamic){
        _state.put(key, value)
        scope.launch {
            // 同步给 toUseState 对象 toMutableState 对象
            mutableSharedFlow.emit(arrayOf(key, value))
        }
    }




    private val _onUpdateCallbackList = mutableListOf<OnUpdateCallback>()

    /**
     * 内部导致 _state 发生更改的时候会调用
     */
    fun onUpdate(cb: OnUpdateCallback): () -> Unit{
        _onUpdateCallbackList.add(cb)
        return {_onUpdateCallbackList.remove(cb)}
    }

    /**
     * 把ViewModelState对的对应的key转为
     * 可以在React中使用,等效于useState功能
     */
    fun <T> toUseState(key: dynamic) = RectState<T>(_state[key] as T, key, this@ViewModelState)
    class RectState<T>(arg: T, private val key: dynamic, val viewModelState: ViewModelState){
        private var state: dynamic = null
        private var setState: dynamic = null

        init {
            val (_state, _setState) = useState<dynamic>(arg)
            state = _state
            setState = _setState
            // 实现viewModelState.set()导致的状态变化
            // 会同步给useState对象
            CoroutineScope(Dispatchers.Default).launch {
                viewModelState.mutableSharedFlow.collect{
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
            viewModelState.set(key, value, true)
            setState(value)
        }

        companion object{

        }
    }

    /**
     * 把ViewModelState对的对应的key转为
     * 可以在Compose中使用,等效于mutableStateOf功能
     */
    @Composable
    fun toMutableStateOf(key: dynamic) = remember {
        val initValue = _state[key]
            ?: throw(Throwable("""
                state[key] == null
                key = $key
                at toMutableStateOf
            """.trimIndent()))
        var mutableState = MutableState.mutableStateMap[key]
        if(mutableState == null){
            mutableState = MutableState(key, _state[key],this@ViewModelState)
            MutableState.mutableStateMap[key] = mutableState
        }
        mutableState
    }


    class MutableState<T>(
        val key: dynamic,
        val initValue: T,
        val viewModelState: ViewModelState
    ) : androidx.compose.runtime.MutableState<T> {
        val mutableState = mutableStateOf<T>(initValue)
        override var value: T
            get() = mutableState.value
            set(value) {
                mutableState.value = value
            }

        override fun component1() = mutableState.component1()
        override fun component2() = mutableState.component2()
        operator fun getValue(thisObj: Any?, property: KProperty<*>): T = value

        operator fun setValue(
            thisObj: Any?, property: KProperty<*>, value: T
        ) {
            viewModelState.set(key, value, true)
            this.value = value
        }

        init{
            CoroutineScope(Dispatchers.Default).launch {
                viewModelState.mutableSharedFlow.collect{
                    if(it[0] == key){
                        this@MutableState.value = it[1]
                    }
                }
            }
        }

        companion object{
            val mutableStateMap = mutableMapOf<Any, MutableState<dynamic>>()
        }
    }
}