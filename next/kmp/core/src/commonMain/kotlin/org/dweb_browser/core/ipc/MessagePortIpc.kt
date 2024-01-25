package org.dweb_browser.core.ipc

import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.getOrElse
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.core.ipc.helper.DWebMessageBytesEncode
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcMessageConst
import org.dweb_browser.core.ipc.helper.IpcPoolMessageArgs
import org.dweb_browser.core.ipc.helper.IpcPoolPack
import org.dweb_browser.core.ipc.helper.PackIpcMessage
import org.dweb_browser.core.ipc.helper.bytesToIpcMessage
import org.dweb_browser.core.ipc.helper.cborToIpcMessage
import org.dweb_browser.core.ipc.helper.cborToIpcPoolPack
import org.dweb_browser.core.ipc.helper.ipcMessageToCbor
import org.dweb_browser.core.ipc.helper.ipcPoolPackToCbor
import org.dweb_browser.core.ipc.helper.ipcPoolPackToJson
import org.dweb_browser.core.ipc.helper.jsonToIpcPack
import org.dweb_browser.core.ipc.helper.jsonToIpcPoolPack
import org.dweb_browser.core.ipc.helper.unByteSpecial
import org.dweb_browser.core.ipc.helper.unStringSpecial
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.ioAsyncExceptionHandler

val debugMessagePortIpc = Debugger("message-port-ipc")

class MessagePort(private val port: IWebMessagePort) {
  companion object {
    private val wm = WeakHashMap<IWebMessagePort, MessagePort>()
    suspend fun from(port: IWebMessagePort): MessagePort =
      wm.getOrPut(port) { MessagePort(port).also { it.installMessageSignal() } }

    val messageScope = CoroutineScope(CoroutineName("webMessage") + ioAsyncExceptionHandler)
  }

  private val messageChannel = Channel<DWebMessage>(capacity = Channel.UNLIMITED)

  private suspend fun installMessageSignal() {
    port.onMessage {
      messageChannel.trySend(it).getOrElse { err ->
        err?.printStackTrace()
      }
    }

  }

  private val _messageSignal by lazy {
    val signal = Signal<DWebMessage>()
    messageScope.launch {
      /// 这里为了确保消息的顺序正确性，比如使用channel来一帧一帧地读取数据，不可以直接用 launch 去异步执行 event，这会导致下层解析数据的顺序问题
      /// 并发性需要到消息被解码出来后才能去执行并发。也就是非 IpcStream 类型的数据才可以走并发
      for (event in messageChannel) {
        signal.emit(event)
      }
      signal.clear()
    }

    signal
  }

  val onWebMessage = _messageSignal.toListener()
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
    messageChannel.close()
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

    private val closeByteArray = "close".toByteArray()
    private val pingByteArray = "ping".toByteArray()
    private val pongByteArray = "pong".toByteArray()
  }

  override fun toString(): String {
    return super.toString() + "@MessagePortIpc($channelId)"
  }

  init {
    port.onWebMessage { event ->
      val ipc = this@MessagePortIpc
      if (event is DWebMessage.DWebMessageString) {
        stringFactory(event)
      } else if (event is DWebMessage.DWebMessageBytes) {
        when (event.encode) {
          DWebMessageBytesEncode.Normal -> when (val message =
            bytesToIpcMessage(event.data, ipc)) {
            closeByteArray -> close()
            pingByteArray -> port.postMessage(pongByteArray)
            pongByteArray -> debugMessagePortIpc("PONG", endpoint.poolId)
            is IpcPoolPack -> {
              debugMessagePortIpc("ON-MESSAGE", "Normal.IpcPoolPack=> $endpoint => $message")
              // 分发消息
              endpoint.emitMessage(IpcPoolMessageArgs(message, ipc))
            }

            else -> throw Exception("unknown message: $message")
          }

          DWebMessageBytesEncode.Cbor -> {
            cborFactory(event)
          }

          else -> {}
        }
      }
    }.removeWhen(onDestroy)
  }

  private suspend fun stringFactory(event: DWebMessage.DWebMessageString) {
    unStringSpecial(event.data)?.let {
      when (it) {
        "close" -> close()
        "ping" -> port.postMessage("pong")
        "pong" -> debugMessagePortIpc("PONG", "${this@MessagePortIpc}")
        else -> throw Exception("unknown message: $it")
      }
      return
    }
    val pack = jsonToIpcPack(event.data)
    val message = jsonToIpcPoolPack(pack.ipcMessageString, this@MessagePortIpc)
    debugMessagePortIpc("ON-MESSAGE", "${pack.pid} => $message")
    // 分发消息
    endpoint.emitMessage(
      IpcPoolMessageArgs(
        IpcPoolPack(pack.pid, message),
        this@MessagePortIpc
      )
    )
  }

  private suspend fun cborFactory(event: DWebMessage.DWebMessageBytes) {
    unByteSpecial(event.data)?.let {
      when (it) {
        IpcMessageConst.closeCborByteArray -> close()
        IpcMessageConst.pingCborByteArray -> port.postMessage(
          IpcMessageConst.pongCborByteArray
        )

        IpcMessageConst.pongCborByteArray -> debugMessagePortIpc("PONG", endpoint.poolId)
        else -> throw Exception("unknown message: $it")
      }
      return
    }
    val pack = cborToIpcPoolPack(event.data)
    val message = cborToIpcMessage(pack.messageByteArray, this@MessagePortIpc)
    debugMessagePortIpc("ON-MESSAGE", "$endpoint => $message")
    // 分发消息
    endpoint.emitMessage(
      IpcPoolMessageArgs(
        IpcPoolPack(pack.pid, message),
        this@MessagePortIpc
      )
    )
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
    this.port.postMessage(message)
  }

  override suspend fun doClose() {
    this.port.close()
    closeSignal.emit()
  }

}

