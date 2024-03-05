package org.dweb_browser.js_frontend.state_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.StateRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.dweb_browser.js_common.state_compose.state.EmitType
import org.dweb_browser.js_common.state_compose.ComposeFlow

@Composable
fun <T: Any, CloseReason: Any> ComposeFlow.ListComposeFlow<T, CloseReason>.toMutableStateListOf(
): MutableStateList<T, CloseReason> {
    return MutableStateList<T, CloseReason>(
        stateCompose = this
    )
}


class MutableStateList<T: Any,  CloseReason: Any>(
    val stateCompose: ComposeFlow.ListComposeFlow<T, CloseReason>,
) {
    val mutableStateList = mutableStateListOf<T>().apply {
        val jobOut = Job()
        var job: Job? = null
        if(stateCompose.stateRoleFlowCore.hasReplay){
             CoroutineScope(Dispatchers.Default + jobOut).launch {
                job = stateCompose.collect{
                    it.forEach { add(it) }
                    job?.cancel()
                    jobOut.cancel()
                }
            }
        }
    }

    var firstStateRecord = mutableStateList.firstStateRecord

    fun prependStateRecord(value: StateRecord)= mutableStateList.prependStateRecord(value)

    fun toList() = mutableStateList.toList()

    val size: Int
        get() = mutableStateList.size

    fun contains(el: T) = mutableStateList.contains(el)
    fun containsAll(els: Collection<T>) = mutableStateList.containsAll(els)

    fun get(index: Int) = mutableStateList.get(index)
    fun indexOf(el: T) = mutableStateList.indexOf(el)
    fun isEmpty() = mutableStateList.isEmpty()
    fun iterator() = mutableStateList.iterator()
    fun lastIndexOf(el: T) = mutableStateList.lastIndexOf(el)
    fun listIterator() = mutableStateList.listIterator()
    fun listIterator(index: Int) = mutableStateList.listIterator(index)
    fun subList(fromIndex: Int, toIndex: Int) = mutableStateList.subList(fromIndex, toIndex)

    suspend fun add(el: T): Boolean{
        console.log("执行了add", el)
        return mutableStateList.add(el).also {
            if(it){
                stateCompose.emitByClient(listOf(el), EmitType.ADD)
            }
        }
    }

    suspend fun add(index: Int, el: T){
        stateCompose.emitByClient(listOf(el), EmitType.ADD_AT, index)
        return mutableStateList.add(index, el)
    }

    suspend fun addAll(els: Collection<T>): Boolean{
        return mutableStateList.addAll(els).also {
            if(it) {
                stateCompose.emitByClient(els.toList(), EmitType.ADD)
            }
        }
    }
    suspend fun addAll(index: Int, els: Collection<T>): Boolean{
        return mutableStateList.addAll(index, els).also {
            if(it){
                stateCompose.emitByClient(els.toList(), EmitType.ADD_AT, index)
            }
        }
    }

    suspend fun clear(){
        mutableStateList.clear().also {
            stateCompose.emitByClient(listOf(), EmitType.CLEAR)
        }
    }

    suspend fun remove(el: T): Boolean{
        return mutableStateList.remove(el).also {
            if(it) stateCompose.emitByClient(listOf(el), EmitType.REMOVE)
        }
    }

    suspend fun removeAll(els: Collection<T>): Boolean{
        return mutableStateList.removeAll(els).also {
            if(it) stateCompose.emitByClient(els.toList(), EmitType.REMOVE)
        }
    }

    suspend fun removeAt(index: Int): T{
        return mutableStateList.removeAt(index).also {
           stateCompose.emitByClient(listOf(), EmitType.REMOVE_AT, index)
        }
    }

    fun retainAll(els: Collection<T>): Boolean{
        return mutableStateList.retainAll(els)
    }

    suspend fun removeRange(fromIndex: Int, toIndex: Int){
        val list = mutableListOf<T>().apply {
            mutableStateList.forEachIndexed{index, t ->
                if(index in fromIndex..<toIndex) this@apply.add(t)
            }
        }
        return mutableStateList.removeRange(fromIndex, toIndex).also {
            stateCompose.emitByClient(list, EmitType.REMOVE)
        }
    }

    // TODO:  mutableStateList 暴露的其他方法添加上即可
    @Composable
    fun forEach(cb: @Composable (T) -> Unit){
        mutableStateList.forEach{
            cb(it)
        }
    }

    init{
        CoroutineScope(Dispatchers.Default).launch {
            stateCompose.collectOperationServer{
                console.log("toMutableListOf 内部监听 stateRoleFlowCore collectClient 的数据变化", it.toString())
                when(it.emitType){
                    EmitType.REPLACE -> {
                        mutableStateList.clear()
                        it.value.forEach { mutableStateList.add(it) }
                    }
                    EmitType.ADD -> {
                        it.value.forEach { mutableStateList.add(it) }
                    }
                    else -> {
                        // TODO: 还需要添加器他的触发类型
                        throw Exception("""还有其他的触发类型没有写""")
                    }
                }
            }
        }
    }
}

