package org.dweb_browser.microservice.ipc

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.printError
import org.dweb_browser.helper.readByteArray
import org.dweb_browser.helper.readInt
import org.dweb_browser.helper.toByteArray
import org.dweb_browser.helper.toUtf8ByteArray
import org.dweb_browser.microservice.help.moshiPack
import org.dweb_browser.microservice.help.types.IMicroModuleManifest
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
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import org.dweb_browser.microservice.ipc.helper.jsonToIpcMessage
import java.io.InputStream


fun debugStreamIpc(tag: String, msg: Any = "", err: Throwable? = null) =
  printDebug("stream-ipc", tag, msg, err)

/**
 * 基于 WebReadableStream 的IPC
 *
 * 它会默认构建出一个输出流，
 * 以及需要手动绑定输入流 {@link bindIncomeStream}
 */
class ReadableStreamIpc(
  override val remote: IMicroModuleManifest,
  override val role: String,
) : Ipc() {
  companion object {
    val incomeStreamCoroutineScope =
      CoroutineScope(CoroutineName("income-stream") + ioAsyncExceptionHandler)
  }

  constructor(
    remote: IMicroModuleManifest,
    role: IPC_ROLE,
  ) : this(remote, role.role)

  override fun toString(): String {
    return super.toString() + "@ReadableStreamIpc(${remote.mmid}, $role)"
  }
  // 虽然 ReadableStreamIpc 支持 Binary 的传输，但是不支持结构化的传输，
  // override val supportBinary: Boolean = true

  private lateinit var controller: ReadableStream.ReadableStreamController

  val stream = ReadableStream(cid = role, onStart = {
    controller = it
  })

  private suspend fun enqueue(data: ByteArray) = controller.enqueue(data)

  private var _incomeStream: InputStream? = null

  private val PONG_DATA by lazy {
    val pong = "pong".toByteArray()
    pong.size.toByteArray() + pong
  }

  class AbortAble {
    val signal = SimpleSignal()
  }

  /**
   * 输入流要额外绑定
   */
  @Synchronized
  fun bindIncomeStream(stream: InputStream, signal: AbortAble = AbortAble()) {
    if (this._incomeStream !== null) {
      throw Exception("in come stream already binded.");
    }
    if (supportMessagePack) {
      throw Exception("还未实现 MessagePack 的编解码能力")
    }

    signal.signal.listen {
      debugStreamIpc("ReadableStreamIpc", "readStream close")
      stream.close()
    }

    val readStream: suspend () -> Unit = {
      try {
        // 如果通道关闭并且没有剩余字节可供读取，则返回 true
        while (stream.available() > 0) {
          val size = stream.readInt()
          if (size <= 0) {
            continue
          }
          debugStreamIpc("size", "$size => $stream")
          // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
          val chunk = stream.readByteArray(size).toString(Charsets.UTF_8)

          val message = jsonToIpcMessage(chunk, this@ReadableStreamIpc)
          when (message) {
            "close" -> close()
            "ping" -> enqueue(PONG_DATA)
            "pong" -> debugStreamIpc("PONG", "$stream")
            is IpcMessage -> {
              debugStreamIpc(
                "ON-MESSAGE", "$size => $message => ${this@ReadableStreamIpc}"
              )
              _messageSignal.emit(IpcMessageArgs(message, this@ReadableStreamIpc))
            }

            else -> throw Exception("unknown message: $message")
          }
        }
        debugStreamIpc("END", "$stream")
      } catch (e: Exception) {
        printError("ReadableStreamIpc", "output stream closed")
      }
      // 流是双向的，对方关闭的时候，自己也要关闭掉
      this.close()
    }
    _incomeStream = stream
    /// 后台执行数据拉取
    incomeStreamCoroutineScope.launch { readStream() }
  }

  override suspend fun _doPostMessage(data: IpcMessage) {
    val message = when {
      supportMessagePack -> moshiPack.packToByteArray(data)
      else -> when (data) {
        is IpcRequest -> Json.encodeToString(data.ipcReqMessage).toUtf8ByteArray()
        is IpcResponse -> Json.encodeToString(data.ipcResMessage).toUtf8ByteArray()
        is IpcStreamData -> Json.encodeToString(data).toUtf8ByteArray()
        is IpcEvent -> Json.encodeToString(data).toUtf8ByteArray()
        is IpcEventJsonAble -> Json.encodeToString(data).toUtf8ByteArray()
        is IpcReqMessage -> Json.encodeToString(data).toUtf8ByteArray()
        is IpcResMessage -> Json.encodeToString(data).toUtf8ByteArray()
        is IpcStreamAbort -> Json.encodeToString(data).toUtf8ByteArray()
        is IpcStreamDataJsonAble -> Json.encodeToString(data).toUtf8ByteArray()
        is IpcStreamEnd -> Json.encodeToString(data).toUtf8ByteArray()
        is IpcStreamPaused -> Json.encodeToString(data).toUtf8ByteArray()
        is IpcStreamPulling -> Json.encodeToString(data).toUtf8ByteArray()
        else -> Json.encodeToString(data).toUtf8ByteArray()
      }
    }
    debugStreamIpc("post", "${message.size} => $stream => $data")
    enqueue(message.size.toByteArray() + message)
  }


  override suspend fun _doClose() {
    controller.close()
    _incomeStream?.close()
  }
}
