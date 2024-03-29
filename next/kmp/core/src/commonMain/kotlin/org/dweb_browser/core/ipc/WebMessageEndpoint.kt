package org.dweb_browser.core.ipc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import org.dweb_browser.core.ipc.helper.CborIpc
import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.core.ipc.helper.EndpointMessage
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.core.ipc.helper.JsonIpc
import org.dweb_browser.core.ipc.helper.cborToIpcPoolPack
import org.dweb_browser.core.ipc.helper.jsonToIpcPoolPack
import org.dweb_browser.core.ipc.helper.serializableIpcMessage
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.withScope

class WebMessageEndpoint(
  override val endpointDebugId: String,
  parentScope: CoroutineScope,
  private val port: IWebMessagePort,
) : IpcEndpoint() {
  override var protocol = EndpointProtocol.Json

  override fun toString() = "WebMessageEndpoint#$endpointDebugId"

  companion object {
    private val wm = WeakHashMap<IWebMessagePort, WebMessageEndpoint>()
    fun from(
      endpointId: String, parentScope: CoroutineScope, port: IWebMessagePort
    ): WebMessageEndpoint = wm.getOrPut(port) {
      WebMessageEndpoint(endpointId, parentScope, port)
    }
  }

  override val scope = parentScope + Job()
  private val messageFlow = MutableSharedFlow<EndpointMessage>()

  override val onMessage = messageFlow.asSharedFlow()

  init {
    scope.launch {
      port.onMessage.collect { event ->
        val packMessage = when (protocol) {
          EndpointProtocol.Json -> jsonToIpcPoolPack(event.text)
          EndpointProtocol.Cbor -> cborToIpcPoolPack(event.binary)
        }
        messageFlow.emit(packMessage)
      }
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
  override suspend fun postMessage(msg: EndpointMessage) {
    withScope(scope) {
      when (protocol) {
        EndpointProtocol.Json -> {
          val data = JsonIpc.encodeToString(serializableIpcMessage(msg))
          port.postMessage(DWebMessage.DWebMessageString(data))
        }

        EndpointProtocol.Cbor -> {
          val data = CborIpc.encodeToByteArray(serializableIpcMessage(msg))
          port.postMessage(DWebMessage.DWebMessageBytes(data))
        }
      }
    }
  }

  init {
    beforeClose = {
      port.close(it)
    }
  }
}