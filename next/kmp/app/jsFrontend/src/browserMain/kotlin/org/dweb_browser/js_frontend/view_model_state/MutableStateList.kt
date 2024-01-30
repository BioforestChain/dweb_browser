//package org.dweb_browser.js_frontend.view_model_state
//
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.mutableStateListOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.snapshots.StateRecord
//import androidx.compose.runtime.toMutableStateList
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//
//// 通过使用 MutableState 实现更新？？
//// 通过设置方法实现？？
//class MyMutableStateList<T>(
//    val key: dynamic,
//    val initValue: Collection<T>,
//    val viewModelState: ViewModelState
//) {
//    val mutableStateList = initValue.toMutableStateList()
//
//    var firstStateRecord = mutableStateList.firstStateRecord
//
//    fun prependStateRecord(value: StateRecord)= mutableStateList.prependStateRecord(value)
//
//    fun toList() = mutableStateList.toList()
//
//    val size: Int
//        get() = mutableStateList.size
//
//    fun contains(el: T) = mutableStateList.contains(el)
//    fun containsAll(els: Collection<T>) = mutableStateList.containsAll(els)
//
//    fun get(index: Int) = mutableStateList.get(index)
//    fun indexOf(el: T) = mutableStateList.indexOf(el)
//    fun isEmpty() = mutableStateList.isEmpty()
//    fun iterator() = mutableStateList.iterator()
//    fun lastIndexOf(el: T) = mutableStateList.lastIndexOf(el)
//    fun listIterator() = mutableStateList.listIterator()
//    fun listIterator(index: Int) = mutableStateList.listIterator(index)
//    fun subList(fromIndex: Int, toIndex: Int) = mutableStateList.subList(fromIndex, toIndex)
//
//    fun add(el: T): Boolean{
//        return mutableStateList.add(el).apply {
//            if(this) console.log("需要同步给Server add: el: ", el)
//        }
//    }
//
//    fun add(index: Int, el: T){
//        return mutableStateList.add(index, el).apply {
//            console.log("需要同步给Server add index el")
//        }
//    }
//
//    fun addAll(els: Collection<T>): Boolean{
//        return mutableStateList.addAll(els).also {
//            if(it) console.log("需要同步给Server addAll els", els)
//
//        }
//    }
//
//    fun addAll(index: Int, els: Collection<T>): Boolean{
//        return mutableStateList.addAll(index, els).also {
//            if(it)  console.log("需要同步给Server addAll index els")
//        }
//    }
//
//    fun clear(){
//        mutableStateList.clear().also {
//            console.log("需要同步给Server clear")
//        }
//    }
//
//    fun remove(el: T): Boolean{
//        return mutableStateList.remove(el).also {
//            if(it) console.log("需要同步给Server remove: ", el)
//        }
//    }
//
//    fun removeAll(els: Collection<T>): Boolean{
//        return mutableStateList.removeAll(els).also {
//            if(it) console.log("需要同步给Server removeAll els: ", els)
//        }
//    }
//
//    fun removeAt(index: Int): T{
//        return mutableStateList.removeAt(index).also {
//            console.log("需要同步给Server it: ", it)
//        }
//    }
//
//    fun retainAll(els: Collection<T>): Boolean{
//        return mutableStateList.retainAll(els).also {
//            if(it) console.log("需要同步给Server retainAll")
//        }
//    }
//
//    fun removeRange(fromIndex: Int, toIndex: Int){
//        return mutableStateList.removeRange(fromIndex, toIndex).also {
//            console.log("需要同步给Server removeRange")
//        }
//    }
//
//    fun set(index: Int, el: T): T{
//        return mutableStateList.set(index, el).also {
//            console.log("需要同步给Server set")
//        }
//    }
//
//
//
//    // TODO:  mutableStateList 暴露的其他方法添加上即可
//    @Composable
//    fun forEach(cb: @Composable (T) -> Unit){
//        mutableStateList.forEach{
//            cb(it)
//        }
//    }
//
//    private fun syncDataToServer(){
//        console.log("需要同步给 server")
//    }
//
//    init{
//        CoroutineScope(Dispatchers.Default).launch {
//            viewModelState.mutableSharedFlow.collect{
//                if(it[0] == key){
//                    this@MutableState.value = it[1]
//                }
//            }
//        }
//    }
//
//    companion object {
//         val mutableStateListMap: MutableMap<dynamic, MyMutableStateList<dynamic>>
//            get() = mutableMapOf<dynamic, MyMutableStateList<dynamic>>()
//
//    }
//}
//
