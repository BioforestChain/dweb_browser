package org.dweb_browser.core.ipc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.core.ipc.helper.cborToEndpointMessage
import org.dweb_browser.core.ipc.helper.jsonToEndpointMessage
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut

open class WebMessageEndpoint(
  override val debugId: String,
  parentScope: CoroutineScope,
  val port: IWebMessagePort,
) : CommonEndpoint(parentScope) {

  override fun toString() = "WebMessageEndpoint@$debugId"

  companion object {
    private val wm = WeakHashMap<IWebMessagePort, WebMessageEndpoint>()
    fun from(
      debugId: String, parentScope: CoroutineScope, port: IWebMessagePort,
    ): WebMessageEndpoint = wm.getOrPut(port) {
      WebMessageEndpoint(debugId, parentScope, port)
    }
  }

  override suspend fun postTextMessage(data: String) {
    port.postMessage(DWebMessage.DWebMessageString(data))
  }

  override suspend fun postBinaryMessage(data: ByteArray) {
    port.postMessage(DWebMessage.DWebMessageBytes(data))
  }

  override suspend fun doStart() {
    super.doStart()
    scope.launch {
      port.onMessage.collect { event ->
        val packMessage =
          if (protocol == EndpointProtocol.CBOR && event is DWebMessage.DWebMessageBytes) {
            cborToEndpointMessage(event.binary)
          } else {
            jsonToEndpointMessage(event.text)
          }
        endpointMsgChannel.send(packMessage)
      }
    }
  }

  init {
    beforeClose = {
      port.close(it)
    }
  }
}