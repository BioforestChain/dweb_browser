package org.dweb_browser.dwebview.polyfill

import io.ktor.websocket.CloseReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.dweb_browser.core.std.dns.httpFetch
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toBase64
import org.dweb_browser.helper.toBase64ByteArray
import org.dweb_browser.helper.toKString
import org.dweb_browser.helper.toNSString
import org.dweb_browser.pure.http.PureBinaryFrame
import org.dweb_browser.pure.http.PureChannelContext
import org.dweb_browser.pure.http.PureFinData
import org.dweb_browser.pure.http.PureTextFrame
import org.dweb_browser.pure.http.websocket
import platform.Foundation.NSArray
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerWithReplyProtocol
import platform.WebKit.WKUserContentController
import platform.darwin.NSObject
import kotlin.coroutines.cancellation.CancellationException

val debugIosWebSocket = Debugger("ios-ws-polyfill")

class DWebViewWebSocketMessageHandler(val engine: DWebViewEngine) : NSObject(),
  WKScriptMessageHandlerWithReplyProtocol {
  private val wsMap = mutableMapOf<Int, PureChannelContext>()

  private class WKScriptMessageEvent(
    private val msgBody: Any,
    private val replyHandler: (Any?, String?) -> Unit
  ) {
    suspend fun consume(handler: suspend (Any) -> Any?) {
      try {
        replyHandler(handler(msgBody), null)
      } catch (e: Throwable) {
        replyHandler(null, e.message ?: "unknown error")
      }
    }
  }

  private val scriptMessageChannel = Channel<WKScriptMessageEvent>()

  init {
    engine.ioScope.launch {
      val pureTextFinData = PureFinData.text()
      val pureBinaryFinData = PureFinData.binary()

      for (event in scriptMessageChannel) {
        event.consume { msgBody ->
          val message = msgBody as NSArray
          val wsId = (message.objectAtIndex(0u) as NSNumber).intValue
          val cmd = (message.objectAtIndex(1u) as NSString).toKString()
          debugIosWebSocket("scriptMessageChannel") { "wsId=$wsId cmd=$cmd message=$message" }

          when (cmd) {
            "connect" -> engine.ioScope.launch {
              try {
                val url = (message.objectAtIndex(2u) as NSString).toKString()
                val pureChannel = httpFetch.client.websocket(url)
                pureChannel.start().apply {
                  wsMap[wsId] = this
                  sendOpen(wsId)
                  for (frame in income) {
                    when (frame) {
                      is PureBinaryFrame -> sendBinaryMessage(wsId, frame.data)
                      is PureTextFrame -> sendTextMessage(wsId, frame.data)
                    }
                  }
                  sendClose(wsId)
                }
              } catch (e: CancellationException) {
                // kotlinx.coroutines.JobCancellationException: Job was cancelled;
                // DWebViewEngine已经destroy触发了ioScope.cancel(null), 注入的脚本已经被销毁，无法send
              } catch (e: Throwable) {
                sendError(wsId, e)
                val foundCode =
                  e.message?.let { msg -> Regex("/\\d+/").find(msg)?.value?.toShort() }
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

            "frame-text" -> {
              wsMap[wsId]!!.run {
                val fin = (message.objectAtIndex(2u) as NSNumber).boolValue
                val data = (message.objectAtIndex(3u) as NSString).toKString()
                pureTextFinData.append(data, fin)?.also {
                  val pureFrame = PureTextFrame(it)
                  outgoing.send(pureFrame)
                }
              }
            }

            "frame-binary" -> {
              wsMap[wsId]!!.run {
                val fin = (message.objectAtIndex(2u) as NSNumber).boolValue
                val data = (message.objectAtIndex(3u) as NSString).toKString().toBase64ByteArray()
                pureBinaryFinData.append(data, fin)?.also {
                  val pureFrame = PureBinaryFrame(it)
                  outgoing.send(pureFrame)
                }
              }
            }

            "close" -> {
              wsMap[wsId]!!.run {
                val reasonCode = (message.objectAtIndex(2u) as NSNumber?)?.shortValue
                val reasonMessage = (message.objectAtIndex(3u) as NSString?)?.toKString()
                close(
                  cause = Throwable((reasonCode ?: CloseReason.Codes.NORMAL.code).toString()),
                  reason = CancellationException(reasonMessage)
                )
              }
            }

            else -> throw Exception("unknown cmd $cmd")
          }
          null
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
      debugIosWebSocket("sendMessage", "wsId=$wsId cmd=$cmd")
      runCatching {
        engine.awaitAsyncJavaScript<Unit>(
          functionBody = "void webkit.messageHandlers.websocket.event.dispatchEvent(new MessageEvent('message',{data:[$wsId,'$cmd',arg1,arg2]}))",
          arguments = arguments,
        )
      }.onFailure {
        debugIosWebSocket("dispatchEvent", "wsId=$wsId cmd=$cmd arg1=$arg1 arg2=$arg2", it)
      }
    }

  private suspend fun sendOpen(wsId: Int) = sendMessage(wsId, "open")
  private suspend fun sendError(wsId: Int, e: Throwable) = sendMessage(wsId, "error", e.message)
  private suspend fun sendClose(wsId: Int, code: Short? = null, reason: String? = null) =
    sendMessage(wsId, "close", code?.toString(), reason)

  private suspend fun sendTextMessage(wsId: Int, data: String) =
    sendMessage(wsId, "message-text", data)

  private suspend fun sendBinaryMessage(wsId: Int, data: ByteArray) =
    sendMessage(wsId, "message-binary", data.toBase64())


  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage,
    replyHandler: (Any?, String?) -> Unit
  ) {
    debugIosWebSocket("didReceiveScriptMessage", "didReceiveScriptMessage=$didReceiveScriptMessage")
    engine.mainScope.launch {
      scriptMessageChannel.send(WKScriptMessageEvent(didReceiveScriptMessage.body, replyHandler))
    }
  }
}