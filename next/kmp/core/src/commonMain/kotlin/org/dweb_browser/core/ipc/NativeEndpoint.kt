package org.dweb_browser.core.ipc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.plus
import org.dweb_browser.core.ipc.helper.EndpointMessage
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.helper.withScope

class NativeMessageChannel(parentScope: CoroutineScope, fromId: String, toId: String) {
  /**
   * 默认锁住，当它解锁的时候，意味着通道关闭
   */
  private val flow1 = MutableSharedFlow<EndpointMessage>()
  private val flow2 = MutableSharedFlow<EndpointMessage>()
  private val scope = parentScope + Job()
  val port1 = NativeEndpoint(scope, flow1, flow2, "<$fromId=>$toId>")
  val port2 = NativeEndpoint(scope, flow2, flow1, "<$toId=>$fromId>")
}

class NativeEndpoint(
  override val scope: CoroutineScope,
  private val messageIn: MutableSharedFlow<EndpointMessage>,
  private val messageOut: MutableSharedFlow<EndpointMessage>,
  override val endpointDebugId: String,
) : IpcEndpoint() {
  override var protocol = EndpointProtocol.Json

  override fun toString() = "NativeEndpoint#$endpointDebugId"

  /**
   * 发送消息
   */
  override suspend fun postMessage(msg: EndpointMessage) {
    withScope(scope) {
      debugNativeIpc("message-out") { "$this >> $msg " }
      messageOut.emit(msg)
    }
  }

  /**
   * 收取消息
   * 这里要用 lazy，因为 messageIn 是使用 BufferOverflow.SUSPEND
   */
  override val onMessage = messageIn.shareIn(scope, SharingStarted.Lazily)
}