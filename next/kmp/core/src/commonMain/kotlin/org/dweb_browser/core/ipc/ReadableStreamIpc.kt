package org.dweb_browser.core.ipc

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.readIntLittleEndian
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.IPC_ROLE
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcMessageArgs
import org.dweb_browser.core.ipc.helper.IpcMessageConst.closeCborByteArray
import org.dweb_browser.core.ipc.helper.IpcMessageConst.pingCborByteArray
import org.dweb_browser.core.ipc.helper.IpcMessageConst.pongCborByteArray
import org.dweb_browser.core.ipc.helper.ReadableStream
import org.dweb_browser.core.ipc.helper.cborToIpcMessage
import org.dweb_browser.core.ipc.helper.ipcMessageToCbor
import org.dweb_browser.core.ipc.helper.ipcMessageToJson
import org.dweb_browser.core.ipc.helper.jsonToIpcMessage
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.toLittleEndianByteArray
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.helper.canRead
import org.dweb_browser.helper.readByteArray


val debugStreamIpc = Debugger("stream-ipc")

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

  val input = ReadableStream(cid = "role=$role<${remote.mmid}>") {
    controller = it
  }

  private suspend fun enqueue(data: ByteArray) = controller.enqueue(data)
  private suspend fun enqueue(vararg dataArray: ByteArray) = controller.enqueue(*dataArray)

  private var _incomeStream: PureStream? = null

  private val PONG_DATA by lazy {
    val pong = "pong".toByteArray()
    pong.size.toLittleEndianByteArray() + pong
  }

  private val CBOR_PONG_DATA by lazy {
    pongCborByteArray.size.toLittleEndianByteArray() + pongCborByteArray
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
    if (isClosed) {
      throw Exception("")
    }
    val reader = stream.getReader("ReadableStreamIpc bindIncomeStream").also { this.reader = it }
    val offAbort = signal.signal.listen {
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
          debugStreamIpc("bindIncomeStream", "size=$size => $stream")
          // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
          val chunk = reader.readPacket(size)
//          debugStreamIpc("bindIncomeStream", "chunk=${chunk.remaining} => $stream")
//          debugStreamIpc("bindIncomeStream", "supportCbor=$supportCbor")

          if (supportCbor) {
            when (val message =
              cborToIpcMessage(chunk.readByteArray(), this@ReadableStreamIpc)) {
              is IpcMessage -> {
//                val logMessage = message.toString().trim()
//                debugStreamIpc("bindIncomeStream", "message=$logMessage => $stream")
//                debugStreamIpc("ON-MESSAGE", "$size => $logMessage => ${this@ReadableStreamIpc}")
                _messageSignal.emit(IpcMessageArgs(message, this@ReadableStreamIpc))
              }

              closeCborByteArray -> close()
              pingCborByteArray -> enqueue(CBOR_PONG_DATA)
              pongCborByteArray -> debugStreamIpc("PONG", "$stream")
              else -> throw Exception("unknown message: $message")
            }
          } else {
            when (val message =
              jsonToIpcMessage(chunk.readByteArray().decodeToString(), this@ReadableStreamIpc)) {
              is IpcMessage -> {
//                val logMessage = message.toString().trim()
//                debugStreamIpc("bindIncomeStream", "message=$logMessage => $stream")
//                debugStreamIpc("ON-MESSAGE", "$size => $logMessage => ${this@ReadableStreamIpc}")
                _messageSignal.emit(IpcMessageArgs(message, this@ReadableStreamIpc))
              }

              "close" -> close()
              "ping" -> enqueue(PONG_DATA)
              "pong" -> debugStreamIpc("PONG", "$stream")
              else -> throw Exception("unknown message: $message")
            }
          }
        }
        debugStreamIpc("END", "$stream")
      } catch (e: Exception) {
        debugStreamIpc("ReadableStreamIpc", "output stream closed:${e.message}")
      }
      // 流是双向的，对方关闭的时候，自己也要关闭掉
      this.close()
    }
    /// 后台执行数据拉取
    incomeStreamCoroutineScope.launch {
      readStream();
      offAbort();
    }
  }

  override suspend fun _doPostMessage(data: IpcMessage) {
    if (supportCbor) {
      val message = ipcMessageToCbor(data)
//      debugStreamIpc("post", "${message.size} => $input => $data")
      enqueue(message.size.toLittleEndianByteArray(), message)// 必须合并起来发送，否则中间可能插入其他写入
      return
    }

    val message = ipcMessageToJson(data).encodeToByteArray()
//    debugStreamIpc("post", "${message.size} => $input => $data")
    enqueue(message.size.toLittleEndianByteArray(), message)// 必须合并起来发送，否则中间可能插入其他写入
  }


  override suspend fun _doClose(): Unit = synchronized(_lock) {
    controller.closeWrite()
    reader?.isClosedForWrite.let {
      reader?.cancel()
    }
  }
}
