package org.dweb_browser.dwebview.ipcWeb

import android.webkit.WebMessage
import android.webkit.WebMessagePort
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.getOrElse
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.help.types.IMicroModuleManifest
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IPC_ROLE
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.dweb_browser.microservice.ipc.helper.IpcEventJsonAble
import org.dweb_browser.microservice.ipc.helper.IpcMessage
import org.dweb_browser.microservice.ipc.helper.IpcMessageArgs
import org.dweb_browser.microservice.ipc.helper.IpcReqMessage
import org.dweb_browser.microservice.ipc.helper.IpcRequest
import org.dweb_browser.microservice.ipc.helper.IpcResMessage
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.ipc.helper.IpcStreamAbort
import org.dweb_browser.microservice.ipc.helper.IpcStreamData
import org.dweb_browser.microservice.ipc.helper.IpcStreamDataJsonAble
import org.dweb_browser.microservice.ipc.helper.IpcStreamEnd
import org.dweb_browser.microservice.ipc.helper.IpcStreamPaused
import org.dweb_browser.microservice.ipc.helper.IpcStreamPulling
import org.dweb_browser.microservice.ipc.helper.jsonToIpcMessage
import java.util.WeakHashMap

fun printThreadName(): String = Thread.currentThread().name
fun debugMessagePortIpc(tag: String, msg: Any = "", err: Throwable? = null) =
  printDebug("message-port-ipc", tag, msg, err)

class MessagePort private constructor(private val port: WebMessagePort) {
  companion object {
    private val wm = WeakHashMap<WebMessagePort, MessagePort>()
    suspend fun from(port: WebMessagePort): MessagePort =
      wm.getOrPut(port) { MessagePort(port).also { it.installMessageSignal() } }

    val messageScope = CoroutineScope(CoroutineName("webMessage") + ioAsyncExceptionHandler)
  }

  val messageChannel = Channel<WebMessage>(capacity = Channel.UNLIMITED)

  private suspend fun installMessageSignal() = withContext(mainAsyncExceptionHandler) {
    port.setWebMessageCallback(object : WebMessagePort.WebMessageCallback() {
      override fun onMessage(port: WebMessagePort, event: WebMessage) {
        messageChannel.trySend(event).getOrElse { err ->
          err?.printStackTrace()
        }
        /// TODO 尝试告知对方暂停，比如发送 StreamPaused
      }
    })
  }

  private val _messageSignal by lazy {
    val signal = Signal<WebMessage>()
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

  fun onWebMessage(cb: Callback<WebMessage>) = _messageSignal.listen(cb)
  suspend fun postMessage(data: String) = withContext(mainAsyncExceptionHandler) {
    port.postMessage(WebMessage(data))
  }

  private var _isClosed = false
  fun close() {
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
  private val roleType: IPC_ROLE,
) : Ipc() {
  companion object {
    suspend fun from(
      port: WebMessagePort, remote: IMicroModuleManifest, roleType: IPC_ROLE
    ) = MessagePortIpc(MessagePort.from(port), remote, roleType)
  }

  override val role get() = roleType.role
  override fun toString(): String {
    return super.toString() + "@MessagePortIpc(${remote.mmid}, $roleType)"
  }

  init {
    port.onWebMessage { event ->
      val ipc = this@MessagePortIpc
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
    }.removeWhen(onDestroy)

  }

  override suspend fun _doPostMessage(data: IpcMessage) {
    val message = when (data) {
      is IpcRequest -> Json.encodeToString(data.ipcReqMessage)
      is IpcResponse -> Json.encodeToString(data.ipcResMessage)
      is IpcStreamData -> Json.encodeToString(data)
      is IpcEvent -> Json.encodeToString(data)
      is IpcEventJsonAble -> Json.encodeToString(data)
      is IpcReqMessage -> Json.encodeToString(data)
      is IpcResMessage -> Json.encodeToString(data)
      is IpcStreamAbort -> Json.encodeToString(data)
      is IpcStreamDataJsonAble -> Json.encodeToString(data)
      is IpcStreamEnd -> Json.encodeToString(data)
      is IpcStreamPaused -> Json.encodeToString(data)
      is IpcStreamPulling -> Json.encodeToString(data)
    }
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

