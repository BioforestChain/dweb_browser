package org.dweb_browser.dwebview.polyfill

import io.ktor.client.plugins.websocket.ws
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.FrameType
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.dweb_browser.core.http.FinData
import org.dweb_browser.core.http.dwebHttpGatewayServer
import org.dweb_browser.core.std.dns.httpFetch
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.toBase64
import org.dweb_browser.helper.toBase64ByteArray
import org.dweb_browser.helper.toKString
import org.dweb_browser.helper.toNSString
import org.dweb_browser.helper.toUtf8
import platform.Foundation.NSArray
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.WebKit.WKContentWorld
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.darwin.NSObject

val debugIosWebSocket = Debugger("ios-ws-polyfill")

class DWebViewWebSocketMessageHandler(val engine: DWebViewEngine) : NSObject(),
  WKScriptMessageHandlerProtocol {
  private val wsMap = mutableMapOf<Int, WebSocketSession>()
  private val scriptMessageChannel = Channel<WKScriptMessage>()

  init {
    engine.ioScope.launch {
      for (didReceiveScriptMessage in scriptMessageChannel) {
        val message = didReceiveScriptMessage.body as NSArray
        val wsId = (message.objectAtIndex(0u) as NSNumber).intValue
        val cmd = (message.objectAtIndex(1u) as NSString).toKString()
        debugIosWebSocket("scriptMessageChannel/in", "wsId=$wsId cmd=$cmd message=$message")

        when (cmd) {
          "connect" -> engine.ioScope.launch {
            try {
              val url = (message.objectAtIndex(2u) as NSString).toKString()
              httpFetch.client.ws("ws://localhost:${dwebHttpGatewayServer.startServer()}?X-Dweb-Url=${url.encodeURIComponent()}") {
                wsMap[wsId] = this@ws
                val opened = launch { sendOpen(wsId) }
                val finBinary =
                  FinData<ByteArray> { list -> list.reduce { acc, bytes -> acc + bytes } }
                val finText =
                  FinData<ByteArray> { list -> list.reduce { acc, bytes -> acc + bytes } }
                for (frame in incoming) {
                  if (!opened.isCompleted) {
                    opened.join()
                  }
                  debugIosWebSocket("incoming", "wsId=$wsId frame=$frame")

                  when (frame.frameType) {
                    FrameType.BINARY -> finBinary.append(frame.data, frame.fin)?.also {
                      sendBinaryMessage(wsId, it)
                    }

                    FrameType.TEXT -> finText.append(frame.data, frame.fin)?.also {
                      sendTextMessage(wsId, it.toUtf8())
                    }

                    FrameType.CLOSE -> (frame as Frame.Close).readReason().also { reason ->
                      sendClose(wsId, reason?.code, reason?.message)
                    }

                    else -> {}
                  }
                }
                sendClose(wsId, null, null)
                debugIosWebSocket("ws-close", "wsId=$wsId")
              }
            } catch (e: Throwable) {
              sendError(wsId, e)
              val foundCode = e.message?.let { msg -> Regex("/\\d+/").find(msg)?.value?.toShort() }
                ?.let { code -> CloseReason.Codes.byCode(code) } ?: CloseReason.Codes.NORMAL
              val foundReason =
                e.message?.let { msg -> Regex("\"(.+)\"").find(msg)?.groupValues?.last() }
              debugIosWebSocket(
                "connect-catch",
                e
              ) {
                "wsId=$wsId foundCode=${foundCode} foundReason=${foundReason})"
              }
              sendClose(wsId, foundCode.code, foundReason ?: foundCode.name)
            }
            wsMap.remove(wsId)
          }

          "message-text" -> {
            wsMap[wsId]?.run {
              val data = (message.objectAtIndex(2u) as NSString).toKString()
              outgoing.send(Frame.Text(data))
            }
          }

          "message-binary" -> {
            wsMap[wsId]?.run {
              val data = (message.objectAtIndex(2u) as NSString).toKString().toBase64ByteArray()
              outgoing.send(Frame.Binary(true, data))
            }
          }

          "close" -> {
            wsMap[wsId]?.run {
              val reasonCode = (message.objectAtIndex(2u) as NSNumber?)?.shortValue
              val reasonMessage = (message.objectAtIndex(3u) as NSString?)?.toKString()
              close(
                CloseReason(reasonCode ?: CloseReason.Codes.NORMAL.code, reasonMessage ?: "")
              )
            }
          }
        }
      }
    }
  }

  private suspend fun sendMessage(
    wsId: Int,
    cmd: String,
    arg1: String? = null,
    arg2: String? = null,
  ) =
    engine.mainScope.launch {
      val arguments: Map<Any?, *> = mapOf(
        "arg1".toNSString() to arg1?.toNSString(),
        "arg2".toNSString() to arg2?.toNSString(),
      )
//      engine.callAsyncJavaScript<Unit>(
//        functionBody = "console.log(`webkit.messageHandlers.websocket.event.dispatchEvent(new MessageEvent('message',{data:[$wsId,'$cmd',arg1,arg2]}))`)",
//        arguments = null
//      )
      engine.awaitAsyncJavaScript(
        functionBody = "void webkit.messageHandlers.websocket.event.dispatchEvent(new MessageEvent('message',{data:[$wsId,'$cmd',arg1,arg2]}))",
        arguments = arguments,
      )
    }

  private suspend fun sendOpen(wsId: Int) = sendMessage(wsId, "open")
  private suspend fun sendError(wsId: Int, e: Throwable) = sendMessage(wsId, "error", e.message)
  private suspend fun sendClose(wsId: Int, code: Short?, reason: String?) =
    sendMessage(wsId, "close", code?.toString(), reason)

  private suspend fun sendTextMessage(wsId: Int, data: String) =
    sendMessage(wsId, "message-text", data)

  private suspend fun sendBinaryMessage(wsId: Int, data: ByteArray) =
    sendMessage(wsId, "message-binary", data.toBase64())


  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage
  ) {
    debugIosWebSocket("didReceiveScriptMessage", "didReceiveScriptMessage=$didReceiveScriptMessage")
    engine.ioScope.launch {
      scriptMessageChannel.send(didReceiveScriptMessage)
    }
  }
}