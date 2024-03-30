package org.dweb_browser.core.ipc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.dweb_browser.core.ipc.helper.EndpointLifecycle
import org.dweb_browser.core.ipc.helper.EndpointIpcMessage
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.withScope

val debugNativeEndpoint = Debugger("nativeEndpoint")

class NativeMessageChannel(parentScope: CoroutineScope, fromId: String, toId: String) {
  /**
   * 默认锁住，当它解锁的时候，意味着通道关闭
   */
  private val messageFlow1 = MutableSharedFlow<EndpointIpcMessage>()
  private val messageFlow2 = MutableSharedFlow<EndpointIpcMessage>()
  private val lifecycleFlow1 = MutableStateFlow<EndpointLifecycle>(EndpointLifecycle.Init())
  private val lifecycleFlow2 = MutableStateFlow<EndpointLifecycle>(EndpointLifecycle.Init())
  private val scope = parentScope + Job()
  val port1 = NativeEndpoint(
    scope, messageFlow1, messageFlow2, lifecycleFlow1, lifecycleFlow2, "<$fromId=>$toId>"
  )
  val port2 = NativeEndpoint(
    scope, messageFlow2, messageFlow1, lifecycleFlow2, lifecycleFlow1, "<$toId=>$fromId>"
  )
}

class NativeEndpoint(
  override val scope: CoroutineScope,
  private val messageIn: SharedFlow<EndpointIpcMessage>,
  private val messageOut: MutableSharedFlow<EndpointIpcMessage>,
  override val lifecycleRemote: StateFlow<EndpointLifecycle>,
  override val lifecycleLocale: MutableStateFlow<EndpointLifecycle>,
  override val endpointDebugId: String,
) : IpcEndpoint() {

  override fun toString() = "NativeEndpoint#$endpointDebugId"

  /**
   * 发送消息
   */
  override suspend fun postMessage(msg: EndpointIpcMessage) {
    awaitOpen()
    withScope(scope) {
      debugNativeEndpoint("message-out") { "$this >> $msg " }
      messageOut.emit(msg)
    }
  }

  /**
   * 收取消息
   * 这里要用 Lazily，因为 messageIn 是使用 BufferOverflow.SUSPEND
   */
  override val onMessage = messageIn.shareIn(scope, SharingStarted.Lazily)

  /**
   * 本地生命周期状态
   * 这里要用 Eagerly，因为是 StateFlow
   */
  override val onLifecycle =
    lifecycleLocale.stateIn(scope, SharingStarted.Eagerly, lifecycleLocale.value)

  override fun getLocaleSubProtocols() = setOf<EndpointProtocol>()

  override suspend fun launchSyncLifecycle() {
    /**
     * NativeEndpoint 的lifecycle是天生双向同步的，所以不需要做任何同步工作
     */
  }
}