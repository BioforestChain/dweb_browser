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
import org.dweb_browser.js_common.view_model.Value
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.safeCast


typealias OnClose<T> = (T) -> Unit

sealed class ComposeFlow<ItemType: Any, ValueType : Any, CloseReason : Any>{
    abstract val itemKClass: KClass<ItemType>
    abstract val valueKClass: KClass<ValueType>
    abstract val stateRoleFlowCore: StateRoleFlowCore<ValueType, CloseReason>
    abstract val operationFlowCore: OperationRoleFlowCore<ValueType, CloseReason>

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

    fun collect(collector: FlowCollector<ValueType>) = stateRoleFlowCore.collect(collector)
    fun collectClient(collector: FlowCollector<ValueType>) = stateRoleFlowCore.collectClient(collector)


    fun collectServer(collector: FlowCollector<ValueType>) = stateRoleFlowCore.collectServer(collector)
    fun collectOperationClient(collector: FlowCollector<OperationValueContainer<ValueType>>) =
        operationFlowCore.collectClient(collector)

    fun collectOperationServer(collector: FlowCollector<OperationValueContainer<ValueType>>) =
        operationFlowCore.collectServer(collector)


    fun encodeToString(value: OperationValueContainer<ValueType>): String {
        return operationFlowCore.serialization.encodeToString(value)
    }


    fun  decodeFromString(v: String): OperationValueContainer<ValueType> = operationFlowCore.serialization.decodeFromString(v)

    abstract fun operationToFlow()
    abstract suspend fun emitByClient(v: ValueType, emitType: EmitType)
    abstract suspend fun emitByClient(v: ValueType, emitType: EmitType, index: Int)

    abstract suspend fun emitByClient(v: Any, emitType: EmitType)
    abstract suspend fun emitByClient(v: Any, emitType: EmitType, index: Int)

    abstract suspend fun emitByServer(v: ValueType, emitType: EmitType)
    abstract suspend fun emitByServer(v: ValueType, emitType: EmitType, index: Int)

    abstract suspend fun emitByServer(v: Any, emitType: EmitType)
    abstract suspend fun emitByServer(v: Any, emitType: EmitType, index: Int)

    /**
     * 打包当前状态为一个操作
     * - 返回一个
     */
    abstract suspend fun packagingCurrentStateOperationValueContainerString(): String?
    class StateComposeFlow<ItemType: Any, ValueType : ItemType, CloseReason : Any>(
        override val itemKClass: KClass<ItemType>,
        override val valueKClass: KClass<ValueType>,
        override val stateRoleFlowCore: StateRoleFlowCore<ValueType, CloseReason>,
        override val operationFlowCore: OperationRoleFlowCore<ValueType, CloseReason>
    ) : ComposeFlow<ItemType, ValueType, CloseReason>() {

        private suspend fun getReplay(): ValueType?{
            val deferred = CompletableDeferred<ValueType?>()
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


        override suspend fun emitByClient(v: ValueType, emitType: EmitType) {
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

        override suspend fun emitByClient(v: Any, emitType: EmitType) {
            emitByClient(valueKClass.cast(v), emitType)
        }

        @Deprecated(
            "Illegal_methods", ReplaceWith("emitByClient(v: Item, emitType: EmitType)")
        )
        override suspend fun emitByClient(v: ValueType, emitType: EmitType, index: Int) {
            throw Exception(
                """
                    Illegal methods, please do not call
                    at StateComposeFlow
                    at ComposeFlow
                """.trimIndent()
            )
        }
        @Deprecated(
            "Illegal_methods", ReplaceWith("emitByClient(v: Any, emitType: EmitType)")
        )
        override suspend fun emitByClient(v: Any, emitType: EmitType, index: Int) {
            emitByClient(valueKClass.cast(v), emitType, index)
        }

        override suspend fun emitByServer(v: ValueType, emitType: EmitType) {
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

        override suspend fun emitByServer(v: Any, emitType: EmitType) = emitByServer(valueKClass.cast(v), emitType)

        @Deprecated(
            "Illegal_methods", ReplaceWith("emitByServer(v: Item, emitType: EmitType)")
        )
        override suspend fun emitByServer(v: ValueType, emitType: EmitType, index: Int) {
            throw Exception(
                """
                    Illegal methods, please do not call
                    at StateComposeFlow
                    at ComposeFlow
                """.trimIndent()
            )
        }
        @Deprecated(
            "Illegal_methods", ReplaceWith("emitByServer(v: Any, emitType: EmitType)")
        )
        override suspend fun emitByServer(v: Any, emitType: EmitType, index: Int) {
            emitByServer(valueKClass.cast(v), emitType, index)
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

    class ListComposeFlow<ItemType: Any, ValueType : List<ItemType>, CloseReason : Any>(
        override val itemKClass: KClass<ItemType>,
        override val valueKClass: KClass<ValueType>,
        override val stateRoleFlowCore: StateRoleFlowCore<ValueType, CloseReason>,
        override val operationFlowCore: OperationRoleFlowCore<ValueType, CloseReason>
    ) : ComposeFlow<ItemType, ValueType, CloseReason>() {
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
        override suspend fun emitByClient(v: ValueType, emitType: EmitType, index: Int) =
            emitAgainAfterConsuming { operationFlowCore.emitByClient(v, emitType, index) }

        override suspend fun emitByClient(v: Any, emitType: EmitType, index: Int) =
            emitByClient(valueKClass.cast(v), emitType, index)

        override suspend fun emitByClient(v: ValueType, emitType: EmitType) {
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

        override suspend fun emitByClient(v: Any, emitType: EmitType) =
            emitByClient(valueKClass.cast(v), emitType)

        override suspend fun emitByServer(v: ValueType, emitType: EmitType) {
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

        override suspend fun emitByServer(v: Any, emitType: EmitType) =
            emitByServer(valueKClass.cast(v), emitType)



        override suspend fun emitByServer(v: ValueType, emitType: EmitType, index: Int) =
            emitAgainAfterConsuming { operationFlowCore.emitByServer(v, emitType, index) }

        override suspend fun emitByServer(v: Any, emitType: EmitType, index: Int) =
            emitByServer(valueKClass.cast(v), emitType, index)

        private suspend fun getReplay(): ValueType {
            val deferred = CompletableDeferred<ValueType>()
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
                valueKClass.cast(listOf<ItemType>()).let { deferred.complete(it) }
            }
            return deferred.await()
        }

        private suspend fun operationToFlowCore(container: OperationValueContainer<ValueType>, role: Role){
            when (container.emitType) {
                EmitType.REPLACE -> stateRoleFlowCore.emit(container.value, role)
                EmitType.ADD -> {
                    val replay = getReplay().toMutableList()
                    container.value.forEach { replay.add(it) }
                    valueKClass.cast(replay).let { stateRoleFlowCore.emit(it, role) }
                }

                EmitType.CLEAR -> {
                    valueKClass.cast(listOf<ItemType>()).let { stateRoleFlowCore.emit(it, role) }
                }

                EmitType.REMOVE -> {
                    val replay = getReplay().toMutableList()
                    container.value.forEach { replay.remove(it) }
                    valueKClass.cast(replay).let{stateRoleFlowCore.emit(it, role)}
                }

                EmitType.REMOVE_AT -> {
                    val replay = getReplay().toMutableList()
                    replay.removeAt(container.index)
                    valueKClass.cast(replay).let { stateRoleFlowCore.emit(it, role) }

                }

                EmitType.ADD_AT -> {
                    val mutableList = mutableListOf<ItemType>()
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
                    valueKClass.cast(mutableList).let { stateRoleFlowCore.emit(it, role) }
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
        inline fun <reified ItemType : Any, reified ValueType: ItemType, CloseReason : Any> createStateComposeFlowInstance(
        ): StateComposeFlow<ItemType, ValueType, CloseReason> {
            return StateComposeFlow<ItemType, ValueType, CloseReason>(
                itemKClass = ItemType::class,
                valueKClass = ValueType::class,
                stateRoleFlowCore = StateRoleFlowCore.createStateRoleFlowCoreInstance<ValueType, CloseReason>(
                ),
                operationFlowCore = OperationRoleFlowCore.createStateOperationRoleFlowCoreInstance<ValueType, CloseReason>()
            )
        }

        inline fun <reified ItemType : Any, reified ValueType: List<ItemType>, CloseReason : Any> createListComposeFlowInstance(
        ): ListComposeFlow<ItemType, ValueType, CloseReason> {
            return ListComposeFlow<ItemType, ValueType, CloseReason>(
                itemKClass = ItemType::class,
                valueKClass = ValueType::class,
                stateRoleFlowCore = StateRoleFlowCore.createStateRoleFlowCoreInstance<ValueType, CloseReason>(
                ),
                operationFlowCore = OperationRoleFlowCore.createStateOperationRoleFlowCoreInstance<ValueType, CloseReason>()
            )
        }
    }
}
