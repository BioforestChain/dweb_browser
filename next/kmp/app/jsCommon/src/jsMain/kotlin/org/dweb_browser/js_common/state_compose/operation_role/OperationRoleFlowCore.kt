package org.dweb_browser.js_common.state_compose.operation_role

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.dweb_browser.js_common.state_compose.close.IClosed
import org.dweb_browser.js_common.state_compose.role.Role
import org.dweb_browser.js_common.state_compose.operation.OperationValueContainer
import org.dweb_browser.js_common.state_compose.serialization.Serialization

class OperationRoleFlowCore<T : Any, CloseReason : Any>(
    override val serialization: Serialization<OperationValueContainer<T>>
) : IStateOperationRoleFlow<T>, IClosed<CloseReason> {
    override val roleFlow: MutableSharedFlow<Role> = MutableSharedFlow(
        replay = 1, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override var currentStateFlowValue: T? = null
    override val stateOperationFlow: MutableSharedFlow<OperationValueContainer<T>> =
        MutableSharedFlow(
            replay = 1, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    override val jobs: MutableList<Job> = mutableListOf<Job>()
    override val stateOperationAndRoleFlowDeferred: CompletableDeferred<Flow<StateOperationAndRoleFlowElement<T>>> =
        CompletableDeferred()
    override val stateOperationClientFlow: MutableSharedFlow<OperationValueContainer<T>> =
        MutableSharedFlow(
            replay = 1, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    override val stateOperationClientStringFlow: MutableSharedFlow<String> = MutableSharedFlow(
        replay = 1, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val stateOperationServerFlow: MutableSharedFlow<OperationValueContainer<T>> =
        MutableSharedFlow(
            replay = 1, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    override val stateOperationServerStringFlow: MutableSharedFlow<String> = MutableSharedFlow(
        replay = 1, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override var hasStateOperationAndRoleFlowConfluence: Boolean = false
    override val closeDeferred: CompletableDeferred<Unit> = CompletableDeferred()
    override val closedListeners: MutableList<(CloseReason?) -> Unit> = mutableListOf()

    /**
     * 关闭
     * 取消全部的收集者
     */
    override fun close(reason: CloseReason?) {
        cancel()
        super.close(reason)
    }

    fun close() {
        close(null)
    }

    companion object {
        inline fun <reified T : Any, CloseReason : Any> createStateOperationRoleFlowCoreInstance(): OperationRoleFlowCore<T, CloseReason> {
            return OperationRoleFlowCore(
                serialization = Serialization.createInstance<OperationValueContainer<T>>()
            )
        }
    }
}