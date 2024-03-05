package org.dweb_browser.js_common.state_compose.state_role

import org.dweb_browser.js_common.state_compose.role.Role
import org.dweb_browser.js_common.state_compose.role.IRoleFlow
import org.dweb_browser.js_common.state_compose.state.IStateFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.launch


class StateAndRoleFlowElement<T>(
    @JsName("state") val state: T, @JsName("role") val role: Role
)

interface IStateRoleFlow<T : Any> : IRoleFlow, IStateFlow<T> {

    val jobs: MutableList<Job>
    val stateAndRoleFlowDeferred: CompletableDeferred<Flow<StateAndRoleFlowElement<T>>>
    val stateClientFlow: MutableSharedFlow<T>
    val stateServerFlow: MutableSharedFlow<T>
    // 初始值必须是false
    var hasReplay: Boolean
    var hasStateAndRoleFlowConfluence: Boolean

    /**
     * 必须要初始化执行这个
     */
    private fun stateAndRoleFlowConfluence(){
        if(!hasStateAndRoleFlowConfluence){
            hasStateAndRoleFlowConfluence = true
            CoroutineScope(Dispatchers.Default).launch {
                stateFlow.combineTransform(roleFlow) { state: T, role: Role ->
                    console.log("stateFlow 发生了变化", state)
                    hasReplay = true
                    emit(
                        StateAndRoleFlowElement(state, role)
                    )
                }.collect{
                    when(it.role){
                        is Role.Client -> stateClientFlow.emit(it.state)
                        is Role.Server -> stateServerFlow.emit(it.state)
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
        collector: FlowCollector<T>
    ): Job {
        val job = CoroutineScope(Dispatchers.Default).launch {
            stateFlow.collect(collector)
        }
        return jobsAdd(job)
    }

    /**
     * 收集客户端角色导致的状态更改
     */
    fun collectClient(
        collector: FlowCollector<T>
    ): Job {
        stateAndRoleFlowConfluence()
        val job = CoroutineScope(Dispatchers.Default).launch {
            stateClientFlow.collect(collector)
        }
        return jobsAdd(job)
    }

    /**
     * 收集服务器端角色导致的状态更改
     */
    fun collectServer(
        collector: FlowCollector<T>
    ): Job {
        stateAndRoleFlowConfluence()
        val job = CoroutineScope(Dispatchers.Default).launch {
            stateServerFlow.collect(collector)
        }
        return jobsAdd(job)
    }

    suspend fun emit(v: T, role: Role) {
        stateFlow.emit(v)
        roleFlow.emit(role)
    }

    suspend fun emitByClient(v: T) {
        stateFlow.emit(v)
        roleFlow.emit(Role.CLIENT)
    }

    suspend fun emitByServer(v: T) {
        stateFlow.emit(v)
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