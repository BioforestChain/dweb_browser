package org.dweb_browser.core.ipc

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readIntLittleEndian
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.ExperimentalSerializationApi
import org.dweb_browser.core.ipc.helper.EndpointMessage
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.core.ipc.helper.ReadableStream
import org.dweb_browser.core.ipc.helper.cborToIpcPoolPack
import org.dweb_browser.core.ipc.helper.ipcPoolPackToCbor
import org.dweb_browser.core.ipc.helper.ipcPoolPackToJson
import org.dweb_browser.core.ipc.helper.jsonToIpcPoolPack
import org.dweb_browser.helper.canRead
import org.dweb_browser.helper.readByteArray
import org.dweb_browser.pure.http.PureStream

class ReadableStreamEndpoint(
  parentScope: CoroutineScope, override val endpointDebugId: String
) : IpcEndpoint() {

  override fun toString() = "ReadableStreamEndpoint#$endpointDebugId"
  override val scope = parentScope + Job()
  var protocol = EndpointProtocol.Json
    private set

  private lateinit var controller: ReadableStream.ReadableStreamController

  val input = ReadableStream(scope, cid = "endpoint") {
    controller = it
  }

  @OptIn(ExperimentalSerializationApi::class)
  override suspend fun postMessage(msg: EndpointMessage) {
    val data = when (protocol) {
      EndpointProtocol.Json -> ipcPoolPackToJson(msg).encodeToByteArray()
      EndpointProtocol.Cbor -> ipcPoolPackToCbor(msg)
    }
    controller.enqueue(data)
  }

  private val messageFlow = MutableSharedFlow<EndpointMessage>()
  override val onMessage = messageFlow.asSharedFlow()


  private var _incomeStream = atomic<PureStream?>(null)
  private var _reader: ByteReadChannel? = null

  /**
   * 输入流要额外绑定
   */
  fun bindIncomeStream(
    stream: PureStream,
  ) = scope.launch {
    _incomeStream.getAndUpdate {
      if (it != null) {
        throw Exception("$endpointDebugId in come stream already binding.");
      }
      if (isClosed) {
        throw Exception("")
      }
      val reader = stream.getReader("ReadableStreamIpc bindIncomeStream").also { _reader = it }

      val readStream = suspend {
        try {
          while (reader.canRead) {
            val size = reader.readIntLittleEndian()
            if (size <= 0) {
              continue
            }
            debugStreamIpc("bindIncomeStream", "$endpointDebugId size=$size => $stream")
            // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
            val packData = reader.readPacket(size).readByteArray()
            debugStreamIpc(
              "bindIncomeStream", "protocol=$protocol,chunk=${packData} => $stream"
            )
            val packMessage = when (protocol) {
              EndpointProtocol.Json -> jsonToIpcPoolPack(packData.decodeToString())
              EndpointProtocol.Cbor -> cborToIpcPoolPack(packData)
            }
            messageFlow.emit(packMessage)
          }
          debugStreamIpc("END", "$stream")
        } catch (e: Exception) {
          debugStreamIpc("ReadableStreamIpc", "output stream closed:${e.message}")
        }
        // 流是双向的，对方关闭的时候，自己也要关闭掉
        this@ReadableStreamEndpoint.doClose()
      }
      /// 后台执行数据拉取
      launch {
        readStream()
      }
      stream
    }
  }


  init {
    beforeClose = {
      controller.closeWrite(it)
      _reader?.cancel(it)
    }
    afterClosed = {
      _incomeStream.update { null }
      _reader = null
    }
  }
}