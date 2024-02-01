//package org.dweb_browser.js_frontend.view_model
//
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.mutableStateListOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.snapshots.StateRecord
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.MutableSharedFlow
//import kotlinx.coroutines.launch
//import org.dweb_browser.js_common.view_model.SyncType
//import react.StateSetter
//import react.useState
//import kotlin.reflect.KProperty
//
//
//typealias OnUpdateCallback = (key: String, value: dynamic, syncType: SyncType) -> Unit
//typealias ViewModelMutableMap  = MutableMap<dynamic, dynamic>
//typealias ViewModelStateFlowContextType = Array<dynamic>
//fun viewModelMutableMapOf(vararg pairs: Pair<dynamic, dynamic>) = mutableMapOf(*pairs)
//class ViewModelState(
//    initState: ViewModelMutableMap? = null
//){
//
//    private val scope = CoroutineScope(Dispatchers.Default)
//    private val mutableSharedFlow = MutableSharedFlow<ViewModelStateFlowContextType>()
//
//    private var _state: ViewModelMutableMap
//    init {
//        _state = initState?: viewModelMutableMapOf()
//    }
//
//    /**
//     * 设置 _state 状态的时候使用
//     * @param key {dynamic}
//     * - 对应 map.key
//     * @param value {dynamic}
//     * - 对应 map.value
//     * @param isInside {Boolean}
//     * - 是否是内部设置
//     * - true 表示是内部设置会执行通过 onUpdate 添加的监听器
//     * - false 表示是外部设置，会同步给 useState mutableStateOf...
//     */
//    fun set(key: dynamic, value:dynamic, isInside: Boolean = false, syncType: SyncType){
//        when(syncType.value){
//            SyncType.REPLACE.value -> _state[key] = value
//            else -> console.error("ViewModelState还有其他的没有处理 $syncType")
//        }
//
//        if(isInside){
//            _onUpdateCallbackList.forEach { cb -> cb(key, value, syncType) }
//        }else{
//            scope.launch {
//                // 同步给 toUseState 对象 toMutableState 对象
//                mutableSharedFlow.emit(arrayOf(key, _state[key]))
//            }
//        }
//    }
//
//    operator fun set(key: dynamic, value:dynamic){
//        set(key, value, false, SyncType.REPLACE)
//    }
//
//    private val _onUpdateCallbackList = mutableListOf<OnUpdateCallback>()
//
//    /**
//     * 内部导致 _state 发生更改的时候会调用
//     */
//    fun onUpdate(cb: OnUpdateCallback): () -> Unit{
//        _onUpdateCallbackList.add(cb)
//        return {_onUpdateCallbackList.remove(cb)}
//    }
//
//    /**
//     * 把ViewModelState对的对应的key转为
//     * 可以在React中使用,等效于useState功能
//     */
//    fun <T> toUseState(key: dynamic) = RectState<T>(_state[key] as T, key, this@ViewModelState)
//    class RectState<T>(arg: T, private val key: dynamic, val viewModelState: ViewModelState){
//        private var state: dynamic = null
//        private var setState: dynamic = null
//
//        init {
//            val (_state, _setState) = useState<dynamic>(arg)
//            state = _state
//            setState = _setState
//            // 实现viewModelState.set()导致的状态变化
//            // 会同步给useState对象
//            CoroutineScope(Dispatchers.Default).launch {
//                viewModelState.mutableSharedFlow.collect{
//                    if(it[0] == key){
//                        setState(it[1])
//                    }
//                }
//            }
//        }
//
//        operator fun component1(): T = state
//
//        operator fun component2(): StateSetter<T> = setState
//
//        operator fun getValue(
//            thisRef: Nothing?,
//            property: KProperty<*>,
//        ): T= state
//        operator fun setValue(
//            thisRef: Nothing?,
//            property: KProperty<*>,
//            value: T,
//        ) {
//            // 设置ViewModel的数据
//            viewModelState.set(key, value, true, SyncType.REPLACE)
//            setState(value)
//        }
//
//        companion object{
//
//        }
//    }
//
//    /**
//     * 把ViewModelState对的对应的key的value转为
//     * 可以在Compose中使用,等效于mutableStateOf功能
//     */
//    @Composable
//    fun toMutableStateOf(key: dynamic) = remember {
//        var mutableState = MutableState.mutableStateMap[key]
//        if(mutableState == null){
//            mutableState = MutableState(key, _state[key],this@ViewModelState)
//            MutableState.mutableStateMap[key] = mutableState
//        }
//        mutableState
//    }
//
//
//    class MutableState<T>(
//        val key: dynamic,
//        val initValue: T,
//        val viewModelState: ViewModelState
//    ){
//        val mutableState = mutableStateOf<T>(initValue)
//
//         var value: T
//            get() = mutableState.value
//            set(value) {
//                mutableState.value = value
//            }
//
//        fun component1() = mutableState.component1()
//        fun component2() = mutableState.component2()
//        operator fun getValue(thisObj: Any?, property: KProperty<*>): T = value
//
//        operator fun setValue(
//            thisObj: Any?, property: KProperty<*>, value: T
//        ) {
//            viewModelState.set(key, value, true, SyncType.REPLACE)
//            this.value = value
//        }
//
//        init{
//            CoroutineScope(Dispatchers.Default).launch {
//                viewModelState.mutableSharedFlow.collect{
//                    if(it[0] == key){
//                        this@MutableState.value = it[1]
//                    }
//                }
//            }
//        }
//
//        companion object{
//            val mutableStateMap = mutableMapOf<Any, MutableState<dynamic>>()
//        }
//    }
//
//    /**
//     * 把ViewModelState对的对应的key的value转为
//     * 可以在Compose中使用,等效于mutableListOf功能
//     */
//    @Composable
//    fun toMutableStateListOf(key: dynamic) = remember {
//        var mutableStateList = MutableStateList.mutableStateListMap[key]
//        console.log(1)
//        if(mutableStateList == null){
//            mutableStateList = MutableStateList(key, _state[key],this@ViewModelState)
//        }
//        mutableStateList
//    }
//
//
//    class MutableStateList<T>(
//        val key: dynamic,
//        initValue: dynamic,
//        val viewModelState: ViewModelState
//    ) {
//        val mutableStateList = mutableStateListOf<T>().apply {
//            initValue.iterator().forEach(::add)
//        }
//
//
//        var firstStateRecord = mutableStateList.firstStateRecord
//
//        fun prependStateRecord(value: StateRecord)= mutableStateList.prependStateRecord(value)
//
//        fun toList() = mutableStateList.toList()
//
//        val size: Int
//            get() = mutableStateList.size
//
//        fun contains(el: T) = mutableStateList.contains(el)
//        fun containsAll(els: Collection<T>) = mutableStateList.containsAll(els)
//
//        fun get(index: Int) = mutableStateList.get(index)
//        fun indexOf(el: T) = mutableStateList.indexOf(el)
//        fun isEmpty() = mutableStateList.isEmpty()
//        fun iterator() = mutableStateList.iterator()
//        fun lastIndexOf(el: T) = mutableStateList.lastIndexOf(el)
//        fun listIterator() = mutableStateList.listIterator()
//        fun listIterator(index: Int) = mutableStateList.listIterator(index)
//        fun subList(fromIndex: Int, toIndex: Int) = mutableStateList.subList(fromIndex, toIndex)
//
//        fun add(el: T): Boolean{
//            return mutableStateList.add(el).also {
//                if(it) {
//                    console.log("需要同步给Server add: el: ", el)
//                    viewModelState.set(key, this@MutableStateList.toList(), true, SyncType.ADD)
//                }
//            }
//        }
//
//        fun add(index: Int, el: T){
//            return mutableStateList.add(index, el).apply {
//                console.log("需要同步给Server add index el")
//            }
//        }
//
//        fun addAll(els: Collection<T>): Boolean{
//            return mutableStateList.addAll(els).also {
//
//                if(it) {
//                    console.log("需要同步给Server addAll els", els)
//                    console.log(mutableStateList.toList())
//                }
//
//            }
//        }
//        fun addAll(index: Int, els: dynamic): Boolean{
//            return mutableStateList.addAll(index, els).also {
//                if(it)  viewModelState.set(key, this@MutableStateList.toList(), true, SyncType.ADD_ALL)
//            }
//        }
//
//        fun clear(){
//            mutableStateList.clear().also {
//                console.log("需要同步给Server clear")
//            }
//        }
//
//        fun remove(el: T): Boolean{
//            return mutableStateList.remove(el).also {
//                if(it) console.log("需要同步给Server remove: ", el)
//            }
//        }
//
//        fun removeAll(els: Collection<T>): Boolean{
//            return mutableStateList.removeAll(els).also {
//                if(it) console.log("需要同步给Server removeAll els: ", els)
//            }
//        }
//
//        fun removeAt(index: Int): T{
//            return mutableStateList.removeAt(index).also {
//                console.log("需要同步给Server it: ", it)
//            }
//        }
//
//        fun retainAll(els: Collection<T>): Boolean{
//            return mutableStateList.retainAll(els).also {
//                if(it) console.log("需要同步给Server retainAll")
//            }
//        }
//
//        fun removeRange(fromIndex: Int, toIndex: Int){
//            return mutableStateList.removeRange(fromIndex, toIndex).also {
//                console.log("需要同步给Server removeRange")
//            }
//        }
//
//        fun set(index: Int, el: T): T{
//            return mutableStateList.set(index, el).also {
//                console.log("需要同步给Server set")
//            }
//        }
//
//
//
//        // TODO:  mutableStateList 暴露的其他方法添加上即可
//        @Composable
//        fun forEach(cb: @Composable (T) -> Unit){
//            mutableStateList.forEach{
//                cb(it)
//            }
//        }
//
//        private fun syncDataToServer(){
//            console.log("需要同步给 server")
//        }
//
//        init{
//            CoroutineScope(Dispatchers.Default).launch {
//                viewModelState.mutableSharedFlow.collect{
//                    console.log("接受到了Server的同步", it)
//                    if(it[0] == key && it[1] is Collection<T>){
//                        console.log("MutableStateList update by server")
//                        this@MutableStateList.clear()
//                        this@MutableStateList.addAll(it[1] as Collection<T>)
//                    }
//                }
//            }
//            console.log("init - 2")
//            mutableStateListMap[key] = this
//        }
//
//        companion object {
//            val mutableStateListMap: MutableMap<dynamic, MutableStateList<dynamic>>
//                get() = mutableMapOf<dynamic, MutableStateList<dynamic>>()
//
//        }
//    }
//}
//
