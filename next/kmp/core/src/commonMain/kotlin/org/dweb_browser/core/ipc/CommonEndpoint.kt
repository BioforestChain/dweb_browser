package org.dweb_browser.core.ipc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.dweb_browser.core.ipc.helper.EndpointIpcMessage
import org.dweb_browser.core.ipc.helper.EndpointIpcRawMessage
import org.dweb_browser.core.ipc.helper.EndpointLifecycle
import org.dweb_browser.core.ipc.helper.EndpointLifecycleInit
import org.dweb_browser.core.ipc.helper.EndpointLifecycleOpened
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.core.ipc.helper.EndpointRawMessage
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.endpointMessageToCbor
import org.dweb_browser.core.ipc.helper.endpointMessageToJson
import org.dweb_browser.core.ipc.helper.toIpcMessage
import org.dweb_browser.helper.OrderInvoker
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.withScope

/**
 * 基于单通讯通道编解码的 通用Endpoint
 * 当前ReadableStream和WebMessage基于这个构建
 */
abstract class CommonEndpoint(
  parentScope: CoroutineScope,
) : IpcEndpoint() {

  /**
   * 默认使用 Json 这种最通用的协议
   * 在一开始的握手阶段会强制使用
   */
  var protocol = EndpointProtocol.JSON
    private set

  final override val scope = parentScope + SupervisorJob()

  /**
   * 单讯息通道
   */
  protected val endpointMsgChannel = Channel<EndpointRawMessage>()

  private val lifecycleRemoteMutableFlow =
    MutableStateFlow(EndpointLifecycle(EndpointLifecycleInit))
  override val lifecycleRemoteFlow = lifecycleRemoteMutableFlow.asStateFlow()


  /**
   * kotlin 环境支持 Cbor 和 Json
   */
  override fun getLocaleSubProtocols() = setOf(EndpointProtocol.CBOR, EndpointProtocol.JSON)
  override suspend fun sendLifecycleToRemote(state: EndpointLifecycle) {
    debugEndpoint.verbose("lifecycle-out") { "${this@CommonEndpoint} >> $state " }
    when (protocol) {
      EndpointProtocol.JSON -> {
        val data = endpointMessageToJson(state)
        postTextMessage(data)
      }

      EndpointProtocol.CBOR -> {
        val data = endpointMessageToCbor(state)
        postBinaryMessage(data)
      }
    }
  }

  private val orderInvoker = OrderInvoker()

  /**
   * 使用协商的结果来进行接下来的通讯
   */
  override suspend fun doStart() {
    lifecycleLocaleFlow.collectIn(scope) { lifecycleLocale ->
      when (val lifecycleState = lifecycleLocale.state) {
        // 握手完成，确定通讯协议
        is EndpointLifecycleOpened -> if (lifecycleState.subProtocols.contains(EndpointProtocol.CBOR)) {
          protocol = EndpointProtocol.CBOR
        }

        else -> {}
      }
    }
    launchJobs += scope.launch {
      for (endpointMessage in endpointMsgChannel) {
        when (endpointMessage) {
          is EndpointLifecycle -> lifecycleRemoteMutableFlow.emit(endpointMessage)
          is EndpointIpcRawMessage -> launch(start = CoroutineStart.UNDISPATCHED) {
            val (pid, ipcMessage) = endpointMessage
            debugEndpoint.verbose("message-in", "pid=$pid ipcMessage=$ipcMessage")
            getIpcMessageProducer(pid).also {
              val ipcMsg = ipcMessage.toIpcMessage(it.ipcDeferred.await())
              it.producer.trySend(ipcMsg)
            }
          }
        }
      }
    }
  }

  /**
   * 发送 EndpointIpcMessage
   */
  override suspend fun postIpcMessage(msg: EndpointIpcMessage) {
    awaitOpen("then-postIpcMessage")
    withScope(scope) {
      debugEndpoint.verbose("message-out", msg)
      when (protocol) {
        EndpointProtocol.JSON -> {
          val data = endpointMessageToJson(msg)
          postTextMessage(data)
        }

        EndpointProtocol.CBOR -> {
          val data = endpointMessageToCbor(msg)
          postBinaryMessage(data)
        }
      }
    }
  }

  /**
   * 发送文本类型的消息
   */
  protected abstract suspend fun postTextMessage(data: String)

  /**
   * 发送二进制类型的消息
   */
  protected abstract suspend fun postBinaryMessage(data: ByteArray)
}