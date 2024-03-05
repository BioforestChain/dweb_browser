package org.dweb_browser.js_common.state_compose.operation_role

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.launch
import org.dweb_browser.js_common.state_compose.role.IRoleFlow
import org.dweb_browser.js_common.state_compose.role.Role
import org.dweb_browser.js_common.state_compose.state.EmitType
import org.dweb_browser.js_common.state_compose.operation.IOperationFlow
import org.dweb_browser.js_common.state_compose.operation.OperationValueContainer
import org.dweb_browser.js_common.state_compose.serialization.Serialization


class StateOperationAndRoleFlowElement<T : Any>(
    @JsName("state") val state: OperationValueContainer<T>, @JsName("role") val role: Role
)

interface IStateOperationRoleFlow<T : Any> : IRoleFlow, IOperationFlow<T> {
    val jobs: MutableList<Job>
    val stateOperationAndRoleFlowDeferred: CompletableDeferred<Flow<StateOperationAndRoleFlowElement<T>>>
    val stateOperationClientFlow: MutableSharedFlow<OperationValueContainer<T>>
    val stateOperationServerFlow: MutableSharedFlow<OperationValueContainer<T>>
    val stateOperationClientStringFlow: MutableSharedFlow<String>
    val stateOperationServerStringFlow: MutableSharedFlow<String>
    var hasStateOperationAndRoleFlowConfluence: Boolean

    private fun stateOperationAndRoleFlowConfluence(){
        if(!hasStateOperationAndRoleFlowConfluence){
            hasStateOperationAndRoleFlowConfluence = true
            CoroutineScope(Dispatchers.Default).launch {
                stateOperationFlow.combineTransform(roleFlow) { state: OperationValueContainer<T>, role: Role ->
                    emit(
                        StateOperationAndRoleFlowElement(state, role)
                    )
                }.collect{
                    when(it.role){
                        is Role.Client -> {
                            stateOperationClientFlow.emit(it.state)
                            stateOperationClientStringFlow.emit(serialization.encodeToString(it.state))
                        }
                        is Role.Server -> {

                            CoroutineScope(Dispatchers.Default).launch {
                                stateOperationServerFlow.emit(it.state)
                            }
                            CoroutineScope(Dispatchers.Default).launch {
                                stateOperationServerStringFlow.emit(serialization.encodeToString(it.state))
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @param collector { FlowCollector<T>} 收集者
     * @return {Job} 取消收集回调的工作
     */
    fun collect(
        collector: FlowCollector<OperationValueContainer<T>>
    ): Job {
        val job = CoroutineScope(Dispatchers.Default).launch {
            stateOperationFlow.collect(collector)
        }
        return jobsAdd(job)
    }


    /**
     * 收集客户端角色导致的状态更改
     */
    fun collectClient(
        collector: FlowCollector<OperationValueContainer<T>>
    ): Job {
        stateOperationAndRoleFlowConfluence()

        val job = CoroutineScope(Dispatchers.Default).launch {
            stateOperationClientFlow.collect(collector)
        }
        return jobsAdd(job)
    }

    /**
     * 收集服务器端角色导致的状态更改
     */
    fun collectServer(
        collector: FlowCollector<OperationValueContainer<T>>
    ): Job {
        stateOperationAndRoleFlowConfluence()
        val job = CoroutineScope(Dispatchers.Default).launch {
            stateOperationServerFlow.collect(collector)
        }
        return jobsAdd(job)
    }

    fun collectClientString(
        collector: FlowCollector<String>
    ): Job{
        stateOperationAndRoleFlowConfluence()
        val job = CoroutineScope(Dispatchers.Default).launch {
            stateOperationClientStringFlow.collect(collector)
        }
        return jobsAdd(job)
    }

    fun collectServerString(
        collector: FlowCollector<String>
    ): Job{
        stateOperationAndRoleFlowConfluence()
        val job = CoroutineScope(Dispatchers.Default).launch {
            stateOperationServerStringFlow.collect(collector)
        }
        return jobsAdd(job)
    }

    private suspend fun emit(v: T, emitType: EmitType, role: Role) {
        super<IOperationFlow>.emit(v, emitType)
        roleFlow.emit(role)
    }

    private suspend fun emit(v: T, emitType: EmitType, index: Int, role: Role) {
        super<IOperationFlow>.emit(v, emitType, index)
        roleFlow.emit(role)
    }

    suspend fun emitByClient(v: T, emitType: EmitType) {
        emit(v, emitType)
        roleFlow.emit(Role.CLIENT)
    }

    suspend fun emitByClient(v: T, emitType: EmitType, index: Int) {
        if(emitType == EmitType.REPLACE) throw Exception("""
            emitType != EmitType.REPLACE
            at emitByClient
            at IOperationRoleFlow
            at IOperationRoleFlow.kt
        """.trimIndent())
        emit(v, emitType, index)
        roleFlow.emit(Role.CLIENT)
    }

    suspend fun emitByServer(v: T, emitType: EmitType) {
        emit(v, emitType)
        roleFlow.emit(Role.SERVER)
    }

    suspend fun emitByServer(v: T, emitType: EmitType, index: Int) {
        emit(v, emitType, index)
        roleFlow.emit(Role.SERVER)
    }

    private fun jobsAdd(job: Job): Job {
        jobs.add(job)
        // job 结束后清理数据
        job.invokeOnCompletion {
            jobs.remove(job)
        }
        return job
    }

    /**
     * 取消全部的收集写成
     */
    fun cancel() {
        jobs.forEach {
            it.cancel()
        }
    }
}