package org.dweb_browser.dwebview.polyfill

import io.ktor.websocket.CloseReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.dweb_browser.core.std.dns.httpFetch
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.base64Binary
import org.dweb_browser.helper.base64String
import org.dweb_browser.helper.toKString
import org.dweb_browser.helper.toNSString
import org.dweb_browser.pure.http.PureBinaryFrame
import org.dweb_browser.pure.http.PureChannelContext
import org.dweb_browser.pure.http.PureFinData
import org.dweb_browser.pure.http.PureTextFrame
import org.dweb_browser.pure.http.websocket
import platform.Foundation.NSArray
import platform.Foundation.NSNull
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
    private val replyHandler: (Any?, String?) -> Unit,
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
    engine.lifecycleScope.launch {
      val pureTextFinData = PureFinData.text()
      val pureBinaryFinData = PureFinData.binary()

      for (event in scriptMessageChannel) {
        event.consume { msgBody ->
          val message = msgBody as NSArray
          val wsId = (message.objectAtIndex(0u) as NSNumber).intValue
          val cmd = (message.objectAtIndex(1u) as NSString).toKString()
          debugIosWebSocket("scriptMessageChannel") { "wsId=$wsId cmd=$cmd message=$message" }

          when (cmd) {
            "connect" -> engine.lifecycleScope.launch {
              try {
                val url = (message.objectAtIndex(2u) as NSString).toKString()
                val pureChannel = httpFetch.client.websocket(url)
                debugIosWebSocket("connect", url)
                pureChannel.start().apply {
                  wsMap[wsId] = this
                  sendOpen(wsId)
                  for (frame in income) {
                    when (frame) {
                      is PureBinaryFrame -> sendBinaryMessage(wsId, frame.binary)
                      is PureTextFrame -> sendTextMessage(wsId, frame.text)
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
                debugIosWebSocket("connect-catch", e) {
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
                  sendText(it)
                }
              }
            }

            "frame-binary" -> {
              wsMap[wsId]!!.run {
                val fin = (message.objectAtIndex(2u) as NSNumber).boolValue
                val data =
                  (message.objectAtIndex(3u) as NSString).toKString().base64Binary
                pureBinaryFinData.append(data, fin)?.also {
                  sendBinary(it)
                }
              }
            }

            "close" -> {
              wsMap[wsId]!!.run {
                val nsNumber = message.objectAtIndex(2u)
                val reasonCode = if (nsNumber is NSNull) {
                  null
                } else {
                  (nsNumber as NSNumber?)?.shortValue
                }
                val nsString = message.objectAtIndex(3u)
                // 用户主动关闭不需要抛出异常，否则会导致PureChannel的close没有正确触发
                if (nsString is NSNull) {
                  close()
                } else {
                  close(
                    cause = Throwable((reasonCode ?: CloseReason.Codes.NORMAL.code).toString()),
//                    reason = CancellationException((nsString as NSString?)?.toKString())
                  )
                }
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
  ) = engine.mainScope.launch {
    val arguments: Map<Any?, *> = mapOf(
      "arg1".toNSString() to arg1?.toNSString(),
      "arg2".toNSString() to arg2?.toNSString(),
    )
    debugIosWebSocket.verbose("sendMessage", "wsId=$wsId cmd=$cmd arg1=$arg1 arg2=$arg2")
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
    sendMessage(wsId, "message-binary", data.base64String)


  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage,
    replyHandler: (Any?, String?) -> Unit,
  ) {
    debugIosWebSocket("didReceiveScriptMessage", "didReceiveScriptMessage=$didReceiveScriptMessage")
    engine.mainScope.launch {
      scriptMessageChannel.send(WKScriptMessageEvent(didReceiveScriptMessage.body, replyHandler))
    }
  }
}