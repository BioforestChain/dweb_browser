package org.dweb_browser.core.ipc

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

  abstract val scope: CoroutineScope
  abstract suspend fun postMessage(msg: EndpointMessage)

  abstract val onMessage: SharedFlow<EndpointMessage>

  // 标记是否启动完成
//  private val localLifecycleFlow = MutableStateFlow<EndpointLifecycle>(EndpointLifecycle.Opening())
  abstract suspend fun postLifecycle(state:EndpointLifecycle)
  abstract val onLifecycle: StateFlow<EndpointLifecycle>

  val lifecycle get() = onLifecycle.value

  // 标记ipc通道是否激活
  val isActivity get() = ENDPOINT_STATE.OPENED == onLifecycle.value.state

  abstract suspend fun start()
  suspend fun awaitOpen() {
    when (val state = onLifecycle.value.state) {
      ENDPOINT_STATE.OPENED -> {}
      ENDPOINT_STATE.OPENING -> {
        onLifecycle.filter { it.state == ENDPOINT_STATE.OPENED }.first()
      }

      else -> {
        throw IllegalStateException("fail to await start, already in $state")
      }
    }
  }

  suspend fun close(cause: CancellationException? = null) = scope.isActive.trueAlso {
    if (scope.coroutineContext[Job] == coroutineContext[Job]) {
      throw Exception("could not close by self")
    }
    doClose(cause)
  }

  val isClosed get() = scope.coroutineContext[Job]!!.isCancelled

  suspend fun awaitClosed() = runCatching {
    scope.coroutineContext[Job]!!.join();
    null
  }.getOrElse { it }
  protected suspend fun doClose(cause: CancellationException? = null) {
    when (lifecycle) {
      is EndpointLifecycle.Opened, is EndpointLifecycle.Opening -> {
        postLifecycle(EndpointLifecycle.Closing())
      }

      is EndpointLifecycle.Closed -> return
      else -> {}
    }
    beforeClose?.invoke(cause)
    postLifecycle(EndpointLifecycle.Closed())
    scope.cancel(cause)
    afterClosed?.invoke(cause)
  }

  protected var beforeClose: (suspend (cause: CancellationException?) -> Unit)? = null
  protected var afterClosed: ((cause: CancellationException?) -> Unit)? = null

}