package org.dweb_browser.core.ipc

import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.core.ipc.helper.ReadableStream
import org.dweb_browser.core.ipc.helper.cborToEndpointMessage
import org.dweb_browser.core.ipc.helper.jsonToEndpointMessage
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.Once1
import org.dweb_browser.helper.canRead
import org.dweb_browser.helper.readByteArray
import org.dweb_browser.helper.readIntLittleEndian
import org.dweb_browser.pure.http.PureStream

val debugStreamEndpoint = Debugger("streamEndpoint")

class ReadableStreamEndpoint(
  parentScope: CoroutineScope,
  override val debugId: String,
) : CommonEndpoint(parentScope) {

  override fun toString() = "ReadableStreamEndpoint@$debugId"

  private lateinit var controller: ReadableStream.ReadableStreamController

  val input = ReadableStream(scope, cid = debugId) {
    controller = it
  }

  private var _incomeStream: PureStream? = null
  private var _reader: ByteReadChannel? = null

  /**
   * 输入流要额外绑定
   */
  val bindIncomeStream = Once1(before = {
    if (_incomeStream != null) {
      throw Exception("$debugId in come stream already binding.");
    }
  }) { stream: PureStream ->
    _incomeStream = stream
    if (isClosed) {
      throw Exception("already closed")
    }
    scope.launch {
      awaitOpen("then-bindIncomeStream")
      val reader = stream.getReader("ReadableStreamIpc bindIncomeStream").also { _reader = it }

      val readStream = suspend {
        try {
          while (reader.canRead) {
            val size = reader.readIntLittleEndian()
            if (size <= 0) {
              continue
            }
            debugStreamEndpoint("bindIncomeStream", "$debugId size=$size => $stream")
            // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
            val packData = reader.readPacket(size).readByteArray()
            debugStreamEndpoint(
              "bindIncomeStream", "protocol=$protocol,chunk=${packData} => $stream"
            )
            val packMessage = when (protocol) {
              EndpointProtocol.JSON -> jsonToEndpointMessage(packData.decodeToString())
              EndpointProtocol.CBOR -> cborToEndpointMessage(packData)
            }
            endpointMsgChannel.send(packMessage)
          }
          debugStreamEndpoint("END", "$stream")
        } catch (e: Exception) {
          debugStreamEndpoint("ReadableStreamIpc", "output stream closed:${e.message}")
        }
        // 流是双向的，对方关闭的时候，自己也要关闭掉
        this@ReadableStreamEndpoint.doClose()
      }
      /// 后台执行数据拉取
      launch {
        readStream()
      }
    }
  }


  override suspend fun postTextMessage(data: String) {
    controller.enqueue(data.encodeToByteArray())
  }

  override suspend fun postBinaryMessage(data: ByteArray) {
    controller.enqueue(data)
  }


  init {
    beforeClose = {
      controller.closeWrite(it)
      _reader?.cancel(it)
    }
    afterClosed = {
      _incomeStream = null
      _reader = null
      bindIncomeStream.reset()
    }
  }
}