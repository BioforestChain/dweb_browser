package org.dweb_browser.js_common.state_compose

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import org.dweb_browser.js_common.state_compose.role.Role
import org.dweb_browser.js_common.state_compose.state.EmitType
import org.dweb_browser.js_common.state_compose.operation.OperationValueContainer
import org.dweb_browser.js_common.state_compose.operation_role.OperationRoleFlowCore
import org.dweb_browser.js_common.state_compose.state_role.StateRoleFlowCore


typealias OnClose<T> = (T) -> Unit

sealed class ComposeFlow<T : Any, CloseReason : Any>() {
    abstract val stateRoleFlowCore: StateRoleFlowCore<T, CloseReason>
    abstract val operationFlowCore: OperationRoleFlowCore<T, CloseReason>

    fun close() {
        stateRoleFlowCore.close()
        operationFlowCore.close()
    }

    fun close(closeReason: CloseReason) {
        stateRoleFlowCore.close(closeReason)
        operationFlowCore.close(closeReason)
    }

    fun onClose(cb: OnClose<CloseReason?>) =
        listOf(stateRoleFlowCore.onClose(cb), operationFlowCore.onClose(cb))

    fun onStateRoleFlowCoreClose(cb: OnClose<CloseReason?>) = stateRoleFlowCore.onClose(cb)

    fun onOperationFlowCoreClose(cb: OnClose<CloseReason?>) =
        operationFlowCore.onClose(cb)

    suspend fun waitClose() {
        stateRoleFlowCore.waitClose()
        operationFlowCore.waitClose()
    }

    suspend fun waitStateRoleFlowCoreClose() = stateRoleFlowCore.waitClose()
    suspend fun waitStateOperationRoleFlowCoreClose() = operationFlowCore.waitClose()

    fun collect(collector: FlowCollector<T>) = stateRoleFlowCore.collect(collector)
    fun collectClient(collector: FlowCollector<T>) = stateRoleFlowCore.collectClient(collector)


    fun collectServer(collector: FlowCollector<T>) = stateRoleFlowCore.collectServer(collector)
    fun collectOperationClient(collector: FlowCollector<OperationValueContainer<T>>) =
        operationFlowCore.collectClient(collector)

    fun collectOperationServer(collector: FlowCollector<OperationValueContainer<T>>) =
        operationFlowCore.collectServer(collector)


    fun encodeToString(value: OperationValueContainer<T>): String {
        return operationFlowCore.serialization.encodeToString(value)
    }


    fun  decodeFromString(v: String): OperationValueContainer<T> = operationFlowCore.serialization.decodeFromString(v)

    abstract fun operationToFlow()
    abstract suspend fun emitByClient(v: T, emitType: EmitType)
    abstract suspend fun emitByClient(v: T, emitType: EmitType, index: Int)
    abstract suspend fun emitByServer(v: T, emitType: EmitType)
    abstract suspend fun emitByServer(v: T, emitType: EmitType, index: Int)

    /**
     * 打包当前状态为一个操作
     * - 返回一个
     */
    abstract suspend fun packagingCurrentStateOperationValueContainerString(): String?
    class StateComposeFlow<Item : Any, CloseReason : Any>(
        override val stateRoleFlowCore: StateRoleFlowCore<Item, CloseReason>,
        override val operationFlowCore: OperationRoleFlowCore<Item, CloseReason>
    ) : ComposeFlow<Item, CloseReason>() {

        private suspend fun getReplay(): Item?{
            val deferred = CompletableDeferred<Item?>()
            val job = Job()
            if(stateRoleFlowCore.hasReplay){
                var jobInside: Job? = null
                CoroutineScope(Dispatchers.Default + job).launch {
                    jobInside = stateRoleFlowCore.collect{
                        console.log(2)
                        deferred.complete(it)
                        job.cancel()
                        jobInside?.cancel()
                    }
                }
            }else{
                deferred.complete(null)
            }
            return deferred.await()
        }

        override suspend fun packagingCurrentStateOperationValueContainerString(): String?{
            val currentValue = getReplay()
            return if(currentValue == null){
                null
            }else{
                operationFlowCore.serialization.encodeToString(OperationValueContainer(
                    index = -1,
                    value = currentValue,
                    emitType = EmitType.REPLACE
                ))
            }
        }


        override suspend fun emitByClient(v: Item, emitType: EmitType) {
            when (emitType) {
                EmitType.REPLACE -> operationFlowCore.emitByClient(v, emitType)
                else -> throw Exception(
                    """
                    Illegal emitType parameter
                    emitType: $emitType
                    at StateComposeFlow
                    at ComposeFlow
                """.trimIndent()
                )
            }
        }

        // TODO: 临时使用
        suspend inline fun <reified T: Any> emitByClient2(v: T, emitType: EmitType){
            when (emitType) {
                EmitType.REPLACE -> operationFlowCore.emitByClient(v as Item, emitType)
                else -> throw Exception(
                    """
                    Illegal emitType parameter
                    emitType: $emitType
                    at StateComposeFlow
                    at ComposeFlow
                """.trimIndent()
                )
            }
        }

        @Deprecated(
            "Illegal_methods", ReplaceWith("emitByClient(v: Item, emitType: EmitType)")
        )
        override suspend fun emitByClient(v: Item, emitType: EmitType, index: Int) {
            throw Exception(
                """
                    Illegal methods, please do not call
                    at StateComposeFlow
                    at ComposeFlow
                """.trimIndent()
            )
        }

        override suspend fun emitByServer(v: Item, emitType: EmitType) {
            when (emitType) {
                EmitType.REPLACE -> operationFlowCore.emitByServer(v, emitType)
                else -> throw Exception(
                    """
                    非法的 emitType 参数
                    emitType: $emitType
                    at StateComposeFlow
                    at ComposeFlow
                """.trimIndent()
                )
            }
        }

        @Deprecated(
            "Illegal_methods", ReplaceWith("emitByServer(v: Item, emitType: EmitType)")
        )
        override suspend fun emitByServer(v: Item, emitType: EmitType, index: Int) {
            throw Exception(
                """
                    Illegal methods, please do not call
                    at StateComposeFlow
                    at ComposeFlow
                """.trimIndent()
            )
        }


        override fun operationToFlow() {
            operationFlowCore.collectClient{
                when (it.emitType) {
                    EmitType.REPLACE -> stateRoleFlowCore.emitByClient(it.value)
                    else -> throw Exception(
                        """
                        非法的 emitType
                        emitType: ${it.emitType}
                        at StateComposeFlow.operationToFlow
                    """.trimIndent()
                    )
                }
            }
            operationFlowCore.collectServer{
                when (it.emitType) {
                    EmitType.REPLACE -> stateRoleFlowCore.emitByServer(it.value)
                    else -> throw Exception(
                        """
                        非法的 emitType
                        emitType: ${it.emitType}
                        at StateComposeFlow.operationToFlow
                    """.trimIndent()
                    )
                }
            }
        }

        init {
            operationToFlow()
        }
    }

    class ListComposeFlow<Item : Any, CloseReason : Any>(
        override val stateRoleFlowCore: StateRoleFlowCore<List<Item>, CloseReason>,
        override val operationFlowCore: OperationRoleFlowCore<List<Item>, CloseReason>
    ) : ComposeFlow<List<Item>, CloseReason>() {
        var canEmitAgain = true
        val emitAgainList = mutableListOf<suspend () -> Unit>()
        // 需要防止连续emit导致collect还没有执行的问题

        override suspend fun packagingCurrentStateOperationValueContainerString(): String?{
            val currentValue = getReplay()
            return if(currentValue.isEmpty()){
                null
            }else{
                operationFlowCore.serialization.encodeToString(OperationValueContainer(
                    index = -1,
                    value = currentValue,
                    emitType = EmitType.REPLACE
                ))
            }
        }

        suspend fun emitAgainAfterConsuming(fn: suspend () -> Unit) {
            if (canEmitAgain) {
                canEmitAgain = false
                fn()
            } else {
                emitAgainList.add(fn)
            }
        }

        /**
         * */
        override suspend fun emitByClient(v: List<Item>, emitType: EmitType, index: Int) =
            emitAgainAfterConsuming { operationFlowCore.emitByClient(v, emitType, index) }

        override suspend fun emitByClient(v: List<Item>, emitType: EmitType) {
            when (emitType) {
                EmitType.REMOVE_AT -> throw Exception(
                    """
                    need index parameter
                    at ListComposeFlow.emitByClient
                """.trimIndent()
                )

                EmitType.ADD_AT -> throw Exception(
                    """
                    need index parameter
                    at ListComposeFlow.emitByClient
                """.trimIndent()
                )
            }
            emitAgainAfterConsuming { operationFlowCore.emitByClient(v, emitType) }
        }


        override suspend fun emitByServer(v: List<Item>, emitType: EmitType) {
            when (emitType) {
                EmitType.REMOVE_AT -> throw Exception(
                    """
                    need index parameter at ListComposeFlow.emitByServer
                """.trimIndent()
                )

                EmitType.ADD_AT -> throw Exception(
                    """
                    need index parameter at ListComposeFlow.emitByServer
                """.trimIndent()
                )
            }
            emitAgainAfterConsuming { operationFlowCore.emitByServer(v, emitType) }
        }


        override suspend fun emitByServer(v: List<Item>, emitType: EmitType, index: Int) =
            emitAgainAfterConsuming { operationFlowCore.emitByServer(v, emitType, index) }


        private suspend fun getReplay(): List<Item> {
            val deferred = CompletableDeferred<List<Item>>()
            val job = Job()
            if (stateRoleFlowCore.hasReplay) {
                var jobInside: Job? = null
                CoroutineScope(Dispatchers.Default + job).launch {
                    jobInside = stateRoleFlowCore.collect {
                        deferred.complete(it)
                        job.cancel()
                        jobInside?.cancel()
                    }
                }
            } else {
                deferred.complete(listOf())
            }
            return deferred.await()
        }

        private suspend fun operationToFlowCore(container: OperationValueContainer<List<Item>>, role: Role){
            when (container.emitType) {
                EmitType.REPLACE -> stateRoleFlowCore.emit(container.value, role)
                EmitType.ADD -> {
                    val replay = getReplay().toMutableList()
                    container.value.forEach { replay.add(it) }

                    stateRoleFlowCore.emit(replay, role)
                }

                EmitType.CLEAR -> {
                    stateRoleFlowCore.emit(listOf(), role)
                }

                EmitType.REMOVE -> {
                    val replay = getReplay().toMutableList()
                    container.value.forEach { replay.remove(it) }
                    stateRoleFlowCore.emit(replay, role)
                }

                EmitType.REMOVE_AT -> {
                    val replay = getReplay().toMutableList()
                    replay.removeAt(container.index)
                    stateRoleFlowCore.emit(replay, role)
                }

                EmitType.ADD_AT -> {
                    val mutableList = mutableListOf<Item>()
                    val replay = getReplay()
                    when {
                        container.index < 0 -> {
                            container.value.forEach { mutableList.add(it) }
                            replay.forEach { mutableList.add(it) }

                        }

                        container.index >= replay.size -> {
                            replay.forEach { mutableList.add(it) }
                            container.value.forEach { mutableList.add(it) }
                        }

                        else -> {
                            replay.forEachIndexed { index, item ->
                                if (index == container.index) {
                                    container.value.forEach { mutableList.add(it) }
                                }
                                mutableList.add(item)
                            }
                        }
                    }
                    stateRoleFlowCore.emit(mutableList, role)
                }

                else -> throw Exception(
                    """
                        非法的 emitType
                        emitType: ${container.emitType}
                        at StateComposeFlow.operationToFlow
                    """.trimIndent()
                )
            }
        }


        override fun operationToFlow() {
            operationFlowCore.collectClient{operationToFlowCore(it, Role.CLIENT)}
            operationFlowCore.collectServer{operationToFlowCore(it, Role.SERVER)}
        }

        init {
            operationToFlow()
            collectClient {
                if (emitAgainList.size != 0) {
                    emitAgainList.removeAt(0)()
                } else {
                    canEmitAgain = true
                }
            }
            collectServer{
                if (emitAgainList.size != 0) {
                    emitAgainList.removeAt(0)()
                } else {
                    canEmitAgain = true
                }
            }
        }
    }

    companion object {
        inline fun <reified T : Any, CloseReason : Any> createStateComposeFlowInstance(
        ): StateComposeFlow<T, CloseReason> {
            return StateComposeFlow<T, CloseReason>(
                stateRoleFlowCore = StateRoleFlowCore.createStateRoleFlowCoreInstance<T, CloseReason>(
                ),
                operationFlowCore = OperationRoleFlowCore.createStateOperationRoleFlowCoreInstance<T, CloseReason>()
            )
        }

        inline fun <reified T : Any, CloseReason : Any> createListComposeFlowInstance(
        ): ListComposeFlow<T, CloseReason> {
            return ListComposeFlow<T, CloseReason>(
                stateRoleFlowCore = StateRoleFlowCore.createStateRoleFlowCoreInstance<List<T>, CloseReason>(
                ),
                operationFlowCore = OperationRoleFlowCore.createStateOperationRoleFlowCoreInstance<List<T>, CloseReason>()
            )
        }
    }
}
