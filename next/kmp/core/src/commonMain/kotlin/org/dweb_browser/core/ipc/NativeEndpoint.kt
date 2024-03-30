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
import org.dweb_browser.core.ipc.helper.EndpointMessage
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.withScope

val debugNativeEndpoint = Debugger("nativeEndpoint")

class NativeMessageChannel(parentScope: CoroutineScope, fromId: String, toId: String) {
  /**
   * 默认锁住，当它解锁的时候，意味着通道关闭
   */
  private val messageFlow1 = MutableSharedFlow<EndpointMessage>()
  private val messageFlow2 = MutableSharedFlow<EndpointMessage>()
  private val sharedLifecycleFlow = MutableStateFlow<EndpointLifecycle>(EndpointLifecycle.Opening())
  private val scope = parentScope + Job()
  val port1 = NativeEndpoint(
    scope,
    messageFlow1,
    messageFlow2,
    sharedLifecycleFlow,
    "<$fromId=>$toId>"
  )
  val port2 = NativeEndpoint(
    scope,
    messageFlow2,
    messageFlow1,
    sharedLifecycleFlow,
    "<$toId=>$fromId>"
  )
}

class NativeEndpoint(
  override val scope: CoroutineScope,
  private val messageIn: SharedFlow<EndpointMessage>,
  private val messageOut: MutableSharedFlow<EndpointMessage>,
  private val sharedLifecycleFlow: MutableStateFlow<EndpointLifecycle>,
  override val endpointDebugId: String,
) : IpcEndpoint() {

  override fun toString() = "NativeEndpoint#$endpointDebugId"

  /**
   * 发送消息
   */
  override suspend fun postMessage(msg: EndpointMessage) {
    withScope(scope) {
      debugNativeEndpoint("message-out") { "$this >> $msg " }
      messageOut.emit(msg)
    }
  }

  /**
   * 收取消息
   * 这里要用 lazy，因为 messageIn 是使用 BufferOverflow.SUSPEND
   */
  override val onMessage = messageIn.shareIn(scope, SharingStarted.Lazily)

  /**
   * 收取消息
   * 这里要用 lazy，因为 messageIn 是使用 BufferOverflow.SUSPEND
   */
  override val onLifecycle = sharedLifecycleFlow.stateIn(scope, SharingStarted.Eagerly, sharedLifecycleFlow.value)
  override suspend fun start() {
    postLifecycle(EndpointLifecycle())
  }

  /**
   * 发送生命周期
   */
  override suspend fun postLifecycle(msg: EndpointLifecycle) {
    withScope(scope) {
      debugNativeEndpoint("lifecycle-out") { "$this >> $msg " }
      sharedLifecycleFlow.emit(msg)
    }
  }
}