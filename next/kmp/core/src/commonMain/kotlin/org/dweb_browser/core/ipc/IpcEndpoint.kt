package org.dweb_browser.core.ipc

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.helper.ENDPOINT_STATE
import org.dweb_browser.core.ipc.helper.EndpointLifecycle
import org.dweb_browser.core.ipc.helper.EndpointMessage
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.helper.trueAlso
import kotlin.coroutines.coroutineContext

/**
 *
 */
abstract class IpcEndpoint {
  abstract val endpointDebugId: String
  abstract var protocol: EndpointProtocol
    protected set

  abstract val scope: CoroutineScope
  abstract suspend fun postMessage(msg: EndpointMessage)
  protected abstract suspend fun syncLifecycle(msg: EndpointLifecycle)

  abstract val onMessage: SharedFlow<EndpointMessage>

  // 标记是否启动完成
  private val stateFlow = MutableStateFlow<EndpointLifecycle>(EndpointLifecycle.Opening())

  val state get() = stateFlow.value
  val onStateChange by lazy { stateFlow.shareIn(scope, SharingStarted.Eagerly) }

  // 标记ipc通道是否激活
  val isActivity get() = ENDPOINT_STATE.OPENED == stateFlow.value.state

  suspend fun awaitOpen() {
    when (val state = stateFlow.value.state) {
      ENDPOINT_STATE.OPENED -> {}
      ENDPOINT_STATE.OPENING -> {
        stateFlow.filter { it.state == ENDPOINT_STATE.OPENED }.first()
      }

      else -> {
        throw IllegalStateException("fail to await start, already in $state")
      }
    }
  }

  val isClosed get() = scope.coroutineContext[Job]!!.isCancelled

  suspend fun awaitClosed() = runCatching {
    scope.coroutineContext[Job]!!.join();
    null
  }.getOrElse { it }

  suspend fun close(cause: CancellationException? = null) = scope.isActive.trueAlso {
    if (scope.coroutineContext[Job] == coroutineContext[Job]) {
      throw Exception("could not close by self")
    }
    doClose(cause)
  }

  protected suspend fun doClose(cause: CancellationException? = null) {
    when (state) {
      is EndpointLifecycle.Opened, is EndpointLifecycle.Opening -> {
        stateFlow.emit(EndpointLifecycle.Closing())
      }

      is EndpointLifecycle.Closed -> return
      else -> {}
    }
    beforeClose?.invoke(cause)
    stateFlow.emit(EndpointLifecycle.Closed())
    scope.cancel(cause)
    afterClosed?.invoke(cause)
  }

  protected var beforeClose: (suspend (cause: CancellationException?) -> Unit)? = null
  protected var afterClosed: ((cause: CancellationException?) -> Unit)? = null

  init {
    scope.launch {
      stateFlow.collect { state ->
        syncLifecycle(state)
      }
    }

  }
}