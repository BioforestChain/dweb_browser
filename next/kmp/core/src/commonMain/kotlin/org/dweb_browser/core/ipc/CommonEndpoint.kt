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
import org.dweb_browser.core.ipc.helper.EndpointLifecycle
import org.dweb_browser.core.ipc.helper.EndpointMessage
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.core.ipc.helper.endpointMessageToCbor
import org.dweb_browser.core.ipc.helper.endpointMessageToJson
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
  var protocol = EndpointProtocol.Json
    private set

  final override val scope = parentScope + SupervisorJob()

  /**
   * 单讯息通道
   */
  protected val endpointMsgChannel = Channel<EndpointMessage>()

  private val lifecycleRemoteMutableFlow =
    MutableStateFlow<EndpointLifecycle>(EndpointLifecycle.Init())
  override val lifecycleRemoteFlow = lifecycleRemoteMutableFlow.asStateFlow()


  /**
   * kotlin 环境支持 Cbor 和 Json
   */
  override fun getLocaleSubProtocols() = setOf(EndpointProtocol.Cbor, EndpointProtocol.Json)
  override suspend fun sendLifecycleToRemote(state: EndpointLifecycle) {
    debugEndpoint("lifecycle-out") { "${this@CommonEndpoint} >> $state " }
    when (protocol) {
      EndpointProtocol.Json -> {
        val data = endpointMessageToJson(state)
        postTextMessage(data)
      }

      EndpointProtocol.Cbor -> {
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
    lifecycleLocaleFlow.collectIn(scope) { state ->
      when (state) {
        // 握手完成，确定通讯协议
        is EndpointLifecycle.Opened -> if (state.subProtocols.contains(EndpointProtocol.Cbor)) {
          protocol = EndpointProtocol.Cbor
        }
        // 即将关闭，
        is EndpointLifecycle.Closing -> {

        }

        else -> {}
      }
    }
    launchJobs += scope.launch {
      for (endpointMessage in endpointMsgChannel) {
        launch(start = CoroutineStart.UNDISPATCHED) {
          orderInvoker.tryInvoke(
            /**
             * EndpointLifecycle 属于 OrderBy
             */
            endpointMessage
          ) {
            when (endpointMessage) {
              is EndpointLifecycle -> lifecycleRemoteMutableFlow.emit(endpointMessage)
              is EndpointIpcMessage -> getIpcMessageProducer(endpointMessage.pid).also {
                it.trySend(endpointMessage.ipcMessage)
              }
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
      when (protocol) {
        EndpointProtocol.Json -> {
          val data = endpointMessageToJson(msg)
          postTextMessage(data)
        }

        EndpointProtocol.Cbor -> {
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
  abstract suspend fun postBinaryMessage(data: ByteArray)

}