package org.dweb_browser.core.ipc

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.helper.ENDPOINT_STATE
import org.dweb_browser.core.ipc.helper.EndpointLifecycle
import org.dweb_browser.core.ipc.helper.EndpointIpcMessage
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.SuspendOnce1
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.withScope
import kotlin.coroutines.coroutineContext

/**
 *
 */
abstract class IpcEndpoint {
  abstract val endpointDebugId: String

  abstract val scope: CoroutineScope
  abstract suspend fun postMessage(msg: EndpointIpcMessage)

  abstract val onMessage: SharedFlow<EndpointIpcMessage>


  /**
   * 更新本地生命周期状态
   */
  protected suspend fun updateLocaleLifecycle(state: EndpointLifecycle) {
    withScope(scope) {
      debugNativeEndpoint("lifecycle-out") { "$this >> $state " }
      lifecycleLocale.emit(state)
    }
  }

  protected abstract val lifecycleLocale: MutableStateFlow<EndpointLifecycle>
  protected abstract val lifecycleRemote: StateFlow<EndpointLifecycle>
  abstract val onLifecycle: StateFlow<EndpointLifecycle>

  val lifecycle get() = onLifecycle.value

  // 标记ipc通道是否激活
  val isActivity get() = ENDPOINT_STATE.OPENED == onLifecycle.value.state

  /**
   * 获取支持的协议，在协商的时候会用到
   */
  protected abstract fun getLocaleSubProtocols(): Set<EndpointProtocol>
  val start = SuspendOnce {
    scope.launch { launchSyncLifecycle() }
    var localeSubProtocols = getLocaleSubProtocols()
    // 当前状态必须是从init开始
    when (val state = lifecycle) {
      is EndpointLifecycle.Init -> {
        updateLocaleLifecycle(EndpointLifecycle.Opening(localeSubProtocols))
      }

      else -> throw IllegalStateException("endpoint state=$state")
    }
    scope.launch {
      // 监听远端生命周期指令，进行协议协商
      lifecycleRemote.collect { state ->
        when (state) {
          is EndpointLifecycle.Closing, is EndpointLifecycle.Closed -> close()
          is EndpointLifecycle.Init, is EndpointLifecycle.Opened -> {}
          // 等收到对方 Opening ，说明对方也开启了，那么开始协商协议，直到一致后才进入 Opened
          is EndpointLifecycle.Opening -> {
            val nextState = if (localeSubProtocols != state.subProtocols) {
              localeSubProtocols = localeSubProtocols.intersect(state.subProtocols)
              EndpointLifecycle.Opening(localeSubProtocols)
            } else {
              EndpointLifecycle.Opened(localeSubProtocols)
            }
            updateLocaleLifecycle(nextState)
          }
        }
      }
    }
  }

  /**
   * 启动生命周期的通讯工作
   */
  protected abstract suspend fun launchSyncLifecycle()
  suspend fun awaitOpen() = when (val state = onLifecycle.value) {
    is EndpointLifecycle.Opened -> state
    is EndpointLifecycle.Opening, is EndpointLifecycle.Init -> {
      onLifecycle.mapNotNull { if (it is EndpointLifecycle.Opened) it else null }.first()
    }

    else -> {
      throw IllegalStateException("fail to await start, already in $state")
    }
  }


  suspend fun close(cause: CancellationException? = null) = scope.isActive.trueAlso {
    closeOnce(cause)
  }

  private val closeOnce = SuspendOnce1 { cause: CancellationException? ->
    if (scope.coroutineContext[Job] == coroutineContext[Job]) {
      WARNING("close endpoint by self. maybe leak.")
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
        updateLocaleLifecycle(EndpointLifecycle.Closing())
      }

      is EndpointLifecycle.Closed -> return
      else -> {}
    }
    beforeClose?.invoke(cause)
    updateLocaleLifecycle(EndpointLifecycle.Closed())
    scope.cancel(cause)
    afterClosed?.invoke(cause)
  }

  protected var beforeClose: (suspend (cause: CancellationException?) -> Unit)? = null
  protected var afterClosed: ((cause: CancellationException?) -> Unit)? = null

}