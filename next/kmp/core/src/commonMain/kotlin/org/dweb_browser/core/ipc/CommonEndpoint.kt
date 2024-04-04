package org.dweb_browser.core.ipc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.ExperimentalSerializationApi
import org.dweb_browser.core.ipc.helper.EndpointIpcMessage
import org.dweb_browser.core.ipc.helper.EndpointLifecycle
import org.dweb_browser.core.ipc.helper.EndpointMessage
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.core.ipc.helper.endpointMessageToCbor
import org.dweb_browser.core.ipc.helper.endpointMessageToJson
import org.dweb_browser.helper.withScope

/**
 * 基于单通讯通道编解码的 通用Endpoint
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
  protected val endpointMsgFlow = MutableSharedFlow<EndpointMessage>()


  override val onIpcMessage = endpointMsgFlow.mapNotNull { if (it is EndpointIpcMessage) it else null }
    .shareIn(scope, SharingStarted.Lazily)
  override val lifecycleRemoteFlow =
    endpointMsgFlow.mapNotNull { if (it is EndpointLifecycle) it else null }
      .stateIn(scope, SharingStarted.Lazily, EndpointLifecycle.Init())


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

  /**
   * 使用协商的结果来进行接下来的通讯
   */
  override suspend fun doStart() {
    super.doStart()
    scope.launch {
      lifecycleLocaleFlow.collect { state ->
        when (state) {
          is EndpointLifecycle.Opened -> if (state.subProtocols.contains(EndpointProtocol.Cbor)) {
            protocol = EndpointProtocol.Cbor
          }

          else -> {}
        }
      }
    }
  }

  /**
   * 发送 EndpointIpcMessage
   */
  @OptIn(ExperimentalSerializationApi::class)
  override suspend fun postIpcMessage(msg: EndpointIpcMessage) {
    awaitOpen("then postIpcMessage")
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