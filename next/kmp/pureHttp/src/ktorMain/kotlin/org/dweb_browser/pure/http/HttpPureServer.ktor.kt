package org.dweb_browser.pure.http

import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSocketUpgrade
import io.ktor.server.websocket.WebSockets
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.ioAsyncExceptionHandler

val debugHttpPureServer = Debugger("httpPureServer")


open class KtorPureServer(
  val serverEngine: ApplicationEngineFactory<*, *>,
  val onRequest: HttpPureServerOnRequest
) {
  private val serverDeferred = CompletableDeferred<ApplicationEngine>()

  suspend fun start(port: UShort): UShort {
    if (!serverDeferred.isCompleted) {
      embeddedServer(serverEngine, port = port.toInt()) {
        install(WebSockets)
        install(createApplicationPlugin("dweb-gateway") {
          onCall { call ->
            withContext(ioAsyncExceptionHandler) {
              /// 将 ktor的request 构建成 pureRequest
              call.request.asPureRequest().also { rawRequest ->
                val url = rawRequest.href
                var pureRequest = rawRequest
                var pureChannelDeferred: CompletableDeferred<PureChannel>? = null
                if (pureRequest.isWebSocket()) {
                  pureRequest = pureRequest.copy(channel = CompletableDeferred<PureChannel>().also {
                    pureChannelDeferred = it
                  })
                }
                val response = onRequest(pureRequest)

                if (pureRequest.hasChannel) {
                  /// 如果是101响应头，那么使用WebSocket来作为双工的通讯标准进行传输，这里使用PureChannel来承担这层抽象
                  when (response.status.value) {
                    /// 如果是200响应头，说明是传统的响应模式，这时候只处理输出，websocket的incoming数据完全忽视
                    200 -> {
                      val res = WebSocketUpgrade(call, null) {
                        val ws = this;
                        val streamReader = response.stream().getReader("Http1Server websocket")
                        /// 将从服务端收到的数据，转成 200 的标准传输到 websocket 的 frame 中
                        launch {
                          streamReader.consumeEachArrayRange { byteArray, _ ->
                            ws.send(Frame.Binary(true, byteArray))
                          }
                          ws.close()
                        }
                        /// 将从客户端收到的数据，但是忽视
                        @Suppress("ControlFlowWithEmptyBody")
                        for (frame in ws.incoming) {// 注意，这里ws.incoming要立刻进行，不能在launch中异步执行，否则ws将无法完成连接建立
                        }
                        /// 等到双工关闭，同时也关闭读取层
                        streamReader.cancel(null)

                      }
                      call.respond(res)
                    }
                    /// 如果是100响应头，说明走的是标准的PureDuplex模型，那么使用WebSocket来作为双工的通讯标准进行传输
                    101 -> {
                      val res = WebSocketUpgrade(call, null) {
                        val ws = this;
                        val income = Channel<PureFrame>()
                        val outgoing = Channel<PureFrame>()
                        val pureChannel = PureChannel(income, outgoing, ws)
                        pureChannelDeferred!!.complete(pureChannel)

                        ws.pipeToPureChannel(url, income, outgoing, pureChannel)
                      }
                      call.respond(res)
                    }

                    else -> call.response.fromPureResponse(response).also {
                      debugHttpPureServer("WS-ERROR", response.body)
                    }
                  }
                } else {
                  call.response.fromPureResponse(response)
                }
              }
            }
          }
        })
      }.also {
        serverDeferred.complete(it)
        it.start(wait = false)
      }
    }

    return getPort()
  }

  private suspend fun getPort() =
    serverDeferred.await().resolvedConnectors().first().port.toUShort()

  suspend fun close() {
    serverDeferred.await().stop()
  }
}
