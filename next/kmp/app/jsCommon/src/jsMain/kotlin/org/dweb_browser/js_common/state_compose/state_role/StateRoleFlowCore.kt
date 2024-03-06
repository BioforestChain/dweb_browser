package org.dweb_browser.js_common.state_compose.state_role

import org.dweb_browser.js_common.state_compose.role.Role
import org.dweb_browser.js_common.state_compose.close.IClosed
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class StateRoleFlowCore<T : Any, CloseReason : Any>(

) : IStateRoleFlow<T>, IClosed<CloseReason> {
    override val jobs: MutableList<Job> = mutableListOf<Job>()
    override val stateAndRoleFlowDeferred: CompletableDeferred<Flow<StateAndRoleFlowElement<T>>> =
        CompletableDeferred()
    override val roleFlow: MutableSharedFlow<Role> = MutableSharedFlow(
        replay = 1, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val stateFlow: MutableSharedFlow<T> = MutableSharedFlow(
        replay = 1, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override var hasStateAndRoleFlowConfluence: Boolean = false

    override var hasReplay: Boolean = false

    override val stateClientFlow: MutableSharedFlow<T> = MutableSharedFlow(
        replay = 1, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val stateServerFlow: MutableSharedFlow<T> = MutableSharedFlow(
        replay = 1, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

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

    init{
        __init__()
    }

    companion object {
        fun <T : Any, CloseReason : Any> createStateRoleFlowCoreInstance(
        ): StateRoleFlowCore<T, CloseReason> {
            return StateRoleFlowCore()
        }
    }
}

