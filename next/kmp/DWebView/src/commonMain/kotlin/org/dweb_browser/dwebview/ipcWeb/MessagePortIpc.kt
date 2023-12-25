package org.dweb_browser.dwebview.ipcWeb

import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.getOrElse
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.IPC_ROLE
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcMessageArgs
import org.dweb_browser.core.ipc.helper.IpcMessageConst.closeCborByteArray
import org.dweb_browser.core.ipc.helper.IpcMessageConst.pingCborByteArray
import org.dweb_browser.core.ipc.helper.IpcMessageConst.pongCborByteArray
import org.dweb_browser.core.ipc.helper.bytesToIpcMessage
import org.dweb_browser.core.ipc.helper.cborToIpcMessage
import org.dweb_browser.core.ipc.helper.ipcMessageToCbor
import org.dweb_browser.core.ipc.helper.ipcMessageToJson
import org.dweb_browser.core.ipc.helper.jsonToIpcMessage
import org.dweb_browser.dwebview.DWebMessage
import org.dweb_browser.dwebview.DWebMessageBytesEncode
import org.dweb_browser.dwebview.IWebMessagePort
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
  open val port: MessagePort,
  override val remote: IMicroModuleManifest,
  private val roleType: IPC_ROLE,
) : Ipc() {
  companion object {
    suspend fun from(
      port: IWebMessagePort, remote: IMicroModuleManifest, roleType: IPC_ROLE
    ) = MessagePortIpc(MessagePort.from(port), remote, roleType)

    private val closeByteArray = "close".toByteArray()
    private val pingByteArray = "ping".toByteArray()
    private val pongByteArray = "pong".toByteArray()
  }

  init {
    port.onWebMessage { event ->
      val ipc = this@MessagePortIpc
      if (event is DWebMessage.DWebMessageString) {
        when (val message = jsonToIpcMessage(event.data, ipc)) {
          "close" -> close()
          "ping" -> port.postMessage("pong")
          "pong" -> debugMessagePortIpc("PONG", "$ipc")
          is IpcMessage -> {
            debugMessagePortIpc("ON-MESSAGE", "$ipc => $message")
            _messageSignal.emit(IpcMessageArgs(message, ipc))
          }

          else -> throw Exception("unknown message: $message")
        }
      } else if (event is DWebMessage.DWebMessageBytes) {
        when (event.encode) {
          DWebMessageBytesEncode.Normal -> when (val message = bytesToIpcMessage(event.data, ipc)) {
            closeByteArray -> close()
            pingByteArray -> port.postMessage(pongByteArray)
            pongByteArray -> debugMessagePortIpc("PONG", "$ipc")
            is IpcMessage -> {
              debugMessagePortIpc("ON-MESSAGE", "$ipc => $message")
              _messageSignal.emit(IpcMessageArgs(message, ipc))
            }

            else -> throw Exception("unknown message: $message")
          }

          DWebMessageBytesEncode.Cbor -> when (val message = cborToIpcMessage(event.data, ipc)) {
            closeCborByteArray -> close()
            pingCborByteArray -> port.postMessage(pongCborByteArray)
            pongCborByteArray -> debugMessagePortIpc("PONG", "$ipc")
            is IpcMessage -> {
              debugMessagePortIpc("ON-MESSAGE", "$ipc => $message")
              _messageSignal.emit(IpcMessageArgs(message, ipc))
            }

            else -> throw Exception("unknown message: $message")
          }

          else -> {}
        }
      }
    }.removeWhen(onDestroy)
  }

  override val role get() = roleType.role
  override fun toString(): String {
    return super.toString() + "@MessagePortIpc(${remote.mmid}, $roleType)"
  }

  override suspend fun _doPostMessage(data: IpcMessage) {
    if (supportCbor) {
      val message = ipcMessageToCbor(data)
      this.port.postMessage(message)
      return
    }

    val message = ipcMessageToJson(data)
    this.port.postMessage(message)
  }

  override suspend fun _doClose() {
    this.port.postMessage("close")
    this.port.close()
  }

  suspend fun emitClose() {
    closeSignal.emit()
  }
}

