package org.dweb_browser.pure.http

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.close
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.websocket.Frame
import js.objects.Object
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import node.buffer.Buffer
import node.http.IncomingMessage
import node.http.ServerResponse
import node.stream.ReadableEvent
import npm.ws.WebSocket
import org.dweb_browser.helper.createByteChannel
import org.dweb_browser.pure.http.ktor.debugPureChannel

fun ServerResponse<*>.writeStatus(
  status: HttpStatusCode,
  customMessage: String? = null
) {
  statusCode = status.value.toDouble()
  statusMessage = customMessage ?: status.description
}

suspend fun incomingMessageAsPureRequest(
  req: IncomingMessage,
  defaultHost: String,
  channel: CompletableDeferred<PureChannel>? = null
): PureServerRequest {
  val reqOrigin = req.headers.host ?: defaultHost
  val reqPath = req.url ?: "/"
  val pureMethod = PureMethod.from(req.method ?: "GET")
  val pureHeaders = PureHeaders().also { pureHeaders ->
    for ((key, values) in Object.entries(req.headersDistinct)) {
      pureHeaders.set(key, values.first())
    }
  }

  val pureRequest = coroutineScope {
    PureServerRequest(
      href = "http://$reqOrigin$reqPath",
      method = pureMethod,
      headers = pureHeaders,
      body = when {
        pureMethod == PureMethod.GET || pureHeaders.get("Content-Length") == "0" -> IPureBody.Empty
        else -> PureStreamBody(createByteChannel().also { reqBodyChannel ->
          val chunkChannel = Channel<ByteArray>(capacity = UNLIMITED)
          req.on(ReadableEvent.DATA) { data: Buffer ->
            chunkChannel.trySend(data.toByteArray())
          }
          req.on(ReadableEvent.CLOSE) {
            chunkChannel.close()
          }
          req.on(ReadableEvent.ERROR) { e ->
            launch {
              reqBodyChannel.close(e)
            }
          }

          launch {
            for (chunk in chunkChannel) {
              reqBodyChannel.writePacket(ByteReadPacket(chunk))
            }
            reqBodyChannel.close()
          }
        })
      },
      channel = channel,
      from = req,
    )
  }
  return pureRequest
}

suspend inline fun IncomingMessage.asPureRequest(
  defaultHost: String,
  channel: CompletableDeferred<PureChannel>? = null
) = incomingMessageAsPureRequest(this, defaultHost, channel)


suspend fun pipeToPureChannel(
  ws: WebSocket.WebSocket, url: String,
  income: Channel<PureFrame>,
  outgoing: Channel<PureFrame>,
  pureChannel: PureChannel,
) = coroutineScope {
  /// 将从 pureChannel 收到的数据，传输到 websocket 的 frame 中
  launch {
    for (pureFrame in outgoing) {
      debugPureChannel.verbose("WebSocketToPureChannel") { "outgoing-to-ws:$pureFrame/$url" }
      val wsFrame = when (pureFrame) {
        is PureTextFrame -> Frame.Text(pureFrame.data)
        is PureBinaryFrame -> Frame.Binary(true, pureFrame.data)
      }
      ws.send(wsFrame)
      debugPureChannel("WebSocketToPureChannel") { "ws-send:$wsFrame/$url" }
    }
    debugPureChannel("WebSocketToPureChannel") { "outgoing-close-ws/$url" }
    ws.close()
  }
  val pureFrameChannel = Channel<PureFrame>(capacity = UNLIMITED)
  /// 将从客户端收到的数据，转成 PureFrame 的标准传输到 pureChannel 中
  ws.on("message") { _, data: Any/* Buffer | ArrayBuffer | Array<Buffer> */, isBinary: Boolean ->
    val messageBuffer = Buffer.from(data)
    val pureFrame: PureFrame = when {
      isBinary -> PureBinaryFrame(messageBuffer.toByteArray())
      else -> PureTextFrame(messageBuffer.toString())
    }
    pureFrameChannel.trySend(pureFrame)
  }
  ws.on("close") { _, code: Number, reason: Buffer ->
    pureFrameChannel.close()
  }
  for (pureFrame in pureFrameChannel) {
    income.send(pureFrame)
    debugPureChannel("WebSocketToPureChannel") { "income-send:$pureFrame/$url" }
  }
  /// 等到双工关闭，同时也关闭channel
  debugPureChannel("WebSocketToPureChannel") { "ws-close-pureChannel/$url" }
  pureChannel.close()
}