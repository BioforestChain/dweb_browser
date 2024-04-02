package org.dweb_browser.core.ipc

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import org.dweb_browser.core.ipc.helper.EndpointIpcMessage
import org.dweb_browser.core.ipc.helper.EndpointLifecycle
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.core.ipc.helper.LIFECYCLE_STATE
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.SuspendOnce1
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.withScope

val debugEndpoint = Debugger("endpoint")

/**
 *
 */
abstract class IpcEndpoint {
  abstract val debugId: String

  abstract val scope: CoroutineScope

  //#region EndpointIpcMessage
  // 这里的设计相对简单，只需要实现 IO 即可

  /**
   * 发送消息
   */
  abstract suspend fun postMessage(msg: EndpointIpcMessage)

  /**
   * 接收消息
   */

  abstract val onMessage: SharedFlow<EndpointIpcMessage>
  //#endregion

  //#region EndpointLifecycle
  // 这里的设计相对复杂，因为提供了内置的生命周期相关的实现，包括 握手、关闭
  // 所以这里的 IO 需要通过子类提供的两个 StateFlow 对象来代表

  /**
   * 本地的生命周期状态流
   */
  val lifecycleLocaleFlow = MutableStateFlow<EndpointLifecycle>(EndpointLifecycle.Init())

  /**
   * 远端的生命周期状态流
   */
  protected abstract val lifecycleRemoteFlow: StateFlow<EndpointLifecycle>

  /**
   * 向远端发送 生命周期 信号
   */

  abstract suspend fun sendLifecycleToRemote(state: EndpointLifecycle)

  /**
   * 生命周期 监听器
   *
   * > 这里要用 Eagerly，因为是 StateFlow
   */
  val onLifecycle by lazy {
    lifecycleLocaleFlow.stateIn(scope, SharingStarted.Eagerly, lifecycleLocaleFlow.value)
  }

  /**
   * 当前生命周期
   */
  val lifecycle get() = lifecycleLocaleFlow.value

  /**
   * 是否处于可以发送消息的状态
   */
  val isActivity get() = LIFECYCLE_STATE.OPENED == lifecycleLocaleFlow.value.state

  /**
   * 获取支持的协议，在协商的时候会用到
   */
  protected abstract fun getLocaleSubProtocols(): Set<EndpointProtocol>

  /**
   * 启动生命周期的相关的工作
   */
  protected open suspend fun doStart() {}

  suspend fun start(await: Boolean = true) {
    withScope(scope) {
      startOnce()
      if (await) {
        awaitOpen()
      }
    }
  }

  /**
   * 启动
   */
  private val startOnce = SuspendOnce {
    doStart()
    var localeSubProtocols = getLocaleSubProtocols()
    // 当前状态必须是从init开始
    when (val state = lifecycle) {
      is EndpointLifecycle.Init -> {
        this@IpcEndpoint.sendLifecycleToRemote(EndpointLifecycle.Opening(localeSubProtocols).also {
          lifecycleLocaleFlow.emit(it)
        })
      }

      else -> throw IllegalStateException("endpoint state=$state")
    }
    debugEndpoint("start", this@IpcEndpoint)
    // 监听远端生命周期指令，进行协议协商
    lifecycleRemoteFlow.collectIn(scope) { state ->
      debugEndpoint("lifecycle-in") { "${this@IpcEndpoint} << $state" }
      when (state) {
        is EndpointLifecycle.Closing, is EndpointLifecycle.Closed -> close()
        // 收到 opened 了，自己也设置成 opened，代表正式握手成功
        is EndpointLifecycle.Opened -> {
          when (val localeState = lifecycleLocaleFlow.value) {
            is EndpointLifecycle.Opening ->
              lifecycleLocaleFlow.emit(EndpointLifecycle.Opened(localeState.subProtocols))

            else -> {}
          }
        }
        // 如果对方是 init，代表刚刚初始化，那么发送目前自己的状态
        is EndpointLifecycle.Init -> this@IpcEndpoint.sendLifecycleToRemote(lifecycleLocaleFlow.value)
        // 等收到对方 Opening ，说明对方也开启了，那么开始协商协议，直到一致后才进入 Opened
        is EndpointLifecycle.Opening -> {
          val nextState = if (localeSubProtocols != state.subProtocols) {
            localeSubProtocols = localeSubProtocols.intersect(state.subProtocols)
            EndpointLifecycle.Opening(localeSubProtocols).also {
              lifecycleLocaleFlow.emit(it)
            }
          } else {
            EndpointLifecycle.Opened(localeSubProtocols)
          }
          this@IpcEndpoint.sendLifecycleToRemote(nextState)
        }
      }
    }
  }

  suspend fun awaitOpen() = lifecycleRemoteFlow.mapNotNull { state ->
    debugEndpoint("awaitOpen-start", state)
    when (state) {
      is EndpointLifecycle.Opened -> state
      is EndpointLifecycle.Closing, is EndpointLifecycle.Closed -> {
        throw IllegalStateException("endpoint already closed")
      }

      else -> null
    }
  }.first().also {
    debugEndpoint("awaitOpen-end", it)
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

  protected open suspend fun doClose(cause: CancellationException? = null) {
    when (lifecycle) {
      is EndpointLifecycle.Opened, is EndpointLifecycle.Opening -> {
        this.sendLifecycleToRemote(EndpointLifecycle.Closing())
      }

      is EndpointLifecycle.Closed -> return
      else -> {}
    }
    beforeClose?.invoke(cause)
    this.sendLifecycleToRemote(EndpointLifecycle.Closed())
    scope.cancel(cause)
    afterClosed?.invoke(cause)
  }

  protected var beforeClose: (suspend (cause: CancellationException?) -> Unit)? = null
  protected var afterClosed: ((cause: CancellationException?) -> Unit)? = null
  //#endregion

}