package org.dweb_browser.core.ipc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.core.ipc.helper.cborToEndpointMessage
import org.dweb_browser.core.ipc.helper.jsonToEndpointMessage

open class WebMessageEndpoint(
  override val debugId: String,
  parentScope: CoroutineScope,
  val port: IWebMessagePort,
) : CommonEndpoint(parentScope) {

  override fun toString() = "WebMessageEndpoint@$debugId"

  override suspend fun postTextMessage(data: String) {
    port.postMessage(DWebMessage.DWebMessageString(data))
  }

  override suspend fun postBinaryMessage(data: ByteArray) {
    port.postMessage(DWebMessage.DWebMessageBytes(data))
  }

  override suspend fun doStart() {
    super.doStart()
    scope.launch {
      for (event in port.onMessage) {
        runCatching {
          val packMessage =
            if (protocol == EndpointProtocol.CBOR && event is DWebMessage.DWebMessageBytes) {
              cborToEndpointMessage(event.binary)
            } else {
              jsonToEndpointMessage(event.text)
            }
          endpointMsgChannel.send(packMessage)
        }.getOrElse { error ->
          debugEndpoint("WebEndpointOnMessage", event, error)
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