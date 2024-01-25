package org.dweb_browser.pure.http.ktor

import io.ktor.websocket.Frame
import io.ktor.websocket.FrameType
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Debugger
import org.dweb_browser.pure.http.PureFinData
import org.dweb_browser.pure.http.PureBinaryFrame
import org.dweb_browser.pure.http.PureChannel
import org.dweb_browser.pure.http.PureFrame
import org.dweb_browser.pure.http.PureTextFrame

val debugPureChannel = Debugger("pureChannel")

suspend fun pipeToPureChannel(
  ws: WebSocketSession, url: String,
  income: Channel<PureFrame>,
  outgoing: Channel<PureFrame>,
  pureChannel: PureChannel,
) = coroutineScope {
  /// 将从 pureChannel 收到的数据，传输到 websocket 的 frame 中
  launch {
    for (pureFrame in outgoing) {
      debugPureChannel("WebSocketToPureChannel") { "outgoing-to-ws:$pureFrame/$url" }
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
  val finBinary =
    PureFinData<ByteArray> { list -> list.reduce { acc, bytes -> acc + bytes } }
  val finText =
    PureFinData<ByteArray> { list -> list.reduce { acc, bytes -> acc + bytes } }
  /// 将从客户端收到的数据，转成 PureFrame 的标准传输到 pureChannel 中
  for (frame in ws.incoming) {// 注意，这里ws.incoming要立刻进行，不能在launch中异步执行，否则ws将无法完成连接建立
    debugPureChannel("WebSocketToPureChannel") { "ws-to-income:$frame/$url" }

    val pureFrame = when (frame.frameType) {
      FrameType.BINARY -> {
        finBinary.append(frame.data, frame.fin)?.let {
          PureBinaryFrame(it)
        }
      }

      FrameType.TEXT -> {
        finText.append(frame.data, frame.fin)?.let {
          PureTextFrame(it.decodeToString())
        }
      }

      FrameType.CLOSE -> break
      else -> continue
    } ?: continue
    income.send(pureFrame)
    debugPureChannel("WebSocketToPureChannel") { "income-send:$pureFrame/$url" }
  }
  /// 等到双工关闭，同时也关闭channel
  debugPureChannel("WebSocketToPureChannel") { "ws-close-pureChannel/$url" }
  pureChannel.close()
}