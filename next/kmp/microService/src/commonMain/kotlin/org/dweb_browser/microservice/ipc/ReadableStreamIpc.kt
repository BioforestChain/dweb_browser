package org.dweb_browser.microservice.ipc

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.readIntLittleEndian
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
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
import org.dweb_browser.helper.toLittleEndianByteArray
import org.dweb_browser.helper.toUtf8
import org.dweb_browser.helper.toUtf8ByteArray
import org.dweb_browser.microservice.help.canRead
import org.dweb_browser.microservice.help.types.IMicroModuleManifest
import org.dweb_browser.microservice.http.PureStream
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

  val input = ReadableStream(cid = role, onStart = {
    controller = it
  })

  private suspend fun enqueue(data: ByteArray) = controller.enqueue(data)
  private suspend fun enqueue(vararg dataArray: ByteArray) = controller.enqueue(*dataArray)

  private var _incomeStream: PureStream? = null

  private val PONG_DATA by lazy {
    val pong = "pong".toByteArray()
    pong.size.toLittleEndianByteArray() + pong
  }

  class AbortAble {
    val signal = SimpleSignal()
  }

  private var reader: ByteReadChannel? = null
  private val _lock = SynchronizedObject()

  /**
   * 输入流要额外绑定
   */
  fun bindIncomeStream(stream: PureStream, signal: AbortAble = AbortAble()) = synchronized(_lock) {
    if (this._incomeStream != null) {
      throw Exception("in come stream already binding.");
    }
    this._incomeStream = stream
    if (supportMessagePack) {
      throw Exception("还未实现 MessagePack 的编解码能力")
    }
    if (isClosed) {
      throw Exception("")
    }
    val reader = stream.getReader("ReadableStreamIpc bindIncomeStream").also { this.reader = it }
    signal.signal.listen {
      debugStreamIpc("ReadableStreamIpc", "readStream close")
      reader.cancel()
    }

    val readStream = suspend {
      try {
        while (reader.canRead) {
          val size = reader.readIntLittleEndian()
          if (size <= 0) {
            continue
          }
          debugStreamIpc("bindIncomeStream/size", "$size => $stream")
          // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
          val chunk = reader.readPacket(size)
          debugStreamIpc("bindIncomeStream/chunk", "${chunk.remaining} => $stream")

          when (val message =
            jsonToIpcMessage(chunk.readByteArray().toUtf8(), this@ReadableStreamIpc)) {
            is IpcMessage -> {
              debugStreamIpc("bindIncomeStream/message", "$message => $stream")
              debugStreamIpc(
                "ON-MESSAGE", "$size => $message => ${this@ReadableStreamIpc}"
              )
              _messageSignal.emit(IpcMessageArgs(message, this@ReadableStreamIpc))
            }

            "close" -> close()
            "ping" -> enqueue(PONG_DATA)
            "pong" -> debugStreamIpc("PONG", "$stream")
            else -> throw Exception("unknown message: $message")
          }
        }
        debugStreamIpc("END", "$stream")
      } catch (e: Exception) {
        printError("ReadableStreamIpc", "output stream closed", e)
      }
      // 流是双向的，对方关闭的时候，自己也要关闭掉
      this.close()
    }
    /// 后台执行数据拉取
    incomeStreamCoroutineScope.launch { readStream() }
  }

  override suspend fun _doPostMessage(data: IpcMessage) {
    if (supportMessagePack) {
      throw Exception("no support support Message Pack")
    }
    val message = when (data) {
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
    }
    debugStreamIpc("post", "${message.size} => $input => $data")
    enqueue(message.size.toLittleEndianByteArray(), message)// 必须合并起来发送，否则中间可能插入其他写入
  }


  override suspend fun _doClose(): Unit = synchronized(_lock) {
    controller.close()
    reader?.cancel()
  }
}
