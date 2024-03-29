package org.dweb_browser.core.ipc

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.core.ipc.helper.DWebMessageBytesEncode
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcPoolMessageArgs
import org.dweb_browser.core.ipc.helper.IpcPoolPack
import org.dweb_browser.core.ipc.helper.PackIpcMessage
import org.dweb_browser.core.ipc.helper.cborToIpcMessage
import org.dweb_browser.core.ipc.helper.cborToIpcPoolPack
import org.dweb_browser.core.ipc.helper.ipcMessageToCbor
import org.dweb_browser.core.ipc.helper.ipcPoolPackToCbor
import org.dweb_browser.core.ipc.helper.ipcPoolPackToJson
import org.dweb_browser.core.ipc.helper.jsonToIpcPack
import org.dweb_browser.core.ipc.helper.jsonToIpcPoolPack
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut

val debugMessagePortIpc = Debugger("message-port-ipc")

class MessagePort(private val port: IWebMessagePort) {
  companion object {
    private val wm = WeakHashMap<IWebMessagePort, MessagePort>()
    suspend fun from(port: IWebMessagePort): MessagePort =
      wm.getOrPut(port) { MessagePort(port).also { it.installMessageSignal() } }
  }

  val webMessageFlow = MutableSharedFlow<DWebMessage>()

  private suspend fun installMessageSignal() {
    port.onMessage {
      webMessageFlow.emit(it)
    }
  }


  suspend fun postMessage(data: String) {
    port.postMessage(DWebMessage.DWebMessageString(data))
  }

  suspend fun postMessage(data: ByteArray) {
    port.postMessage(DWebMessage.DWebMessageBytes(data))
  }

  private var _isClosed = false
  suspend fun close() {
    if (_isClosed) {
      return
    }
    _isClosed = true
    port.close()
  }
}


open class MessagePortIpc(
  val port: MessagePort,
  override val remote: IMicroModuleManifest,
  channelId: String,
  endpoint: IpcPool
) : Ipc(channelId, endpoint) {
  companion object {
    suspend fun from(
      port: IWebMessagePort,
      remote: IMicroModuleManifest,
      channelId: String,
      endpoint: IpcPool
    ) = MessagePortIpc(MessagePort.from(port), remote, channelId, endpoint)

  }

  override fun toString(): String {
    return super.toString() + "@MessagePortIpc($channelId)"
  }

  init {
    ipcScope.launch {
      port.webMessageFlow.collect { event ->
        val ipc = this@MessagePortIpc

        val packMessage = when (event) {
          is DWebMessage.DWebMessageString -> {
            val pack = jsonToIpcPack(event.data)
            val message = jsonToIpcPoolPack(pack.ipcMessage, this@MessagePortIpc)
            debugMessagePortIpc("ON-MESSAGE string") { "$channelId => $message" }
            IpcPoolPack(pack.pid, message)
          }

          is DWebMessage.DWebMessageBytes -> {
            when (event.encode) {
              DWebMessageBytesEncode.Normal -> {
                val pack = jsonToIpcPack(event.data.decodeToString())
                val message = jsonToIpcPoolPack(pack.ipcMessage, ipc)
                debugMessagePortIpc("ON-MESSAGE json") { "$endpoint => $message" }
                IpcPoolPack(pack.pid, message)
              }

              DWebMessageBytesEncode.Cbor -> {
                val pack = cborToIpcPoolPack(event.data)
                val message = cborToIpcMessage(pack.messageByteArray, this@MessagePortIpc)
                debugMessagePortIpc("ON-MESSAGE cbor") { "$endpoint => $message" }
                IpcPoolPack(pack.pid, message)
              }
            }
          }
        }

        endpoint.dispatchMessage(IpcPoolMessageArgs(packMessage, ipc))
      }
    }
  }

  override suspend fun doPostMessage(pid: Int, data: IpcMessage) {
    if (supportCbor) {
      val message = ipcMessageToCbor(data)
      val pack = ipcPoolPackToCbor(PackIpcMessage(pid, message))
      this.port.postMessage(pack)
      return
    }
    // 普通信号
    val message = ipcPoolPackToJson(IpcPoolPack(pid, data))
    try {
      this.port.postMessage(message)
    } catch (e: Exception) {
      println("xpostMessage=> $channelId $data ${e.message}")
    }
  }

  override suspend fun _doClose() {
    this.port.close()
  }

}

