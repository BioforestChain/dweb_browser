package org.dweb_browser.shared

import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSocketUpgrade
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dweb_browser.core.help.asPureRequest
import org.dweb_browser.core.help.fromPureResponse
import org.dweb_browser.core.help.isWebSocket
import org.dweb_browser.core.ipc.helper.ReadableStream
import org.dweb_browser.core.std.http.debugHttp
import org.dweb_browser.core.std.http.findDwebGateway
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.engine.getKtorServerEngine

class Http1ServerTest {
  companion object {
    const val PREFIX = "http://";
    const val PROTOCOL = "http:";
    const val PORT = 80;
  }

  private var bindingPort = 20222

  private var server: ApplicationEngine? = null

  suspend fun createServer(
    httpHandler: suspend (PureClientRequest) -> PureResponse,
  ) {
    if (server != null) {
      throw Exception("server alter created")
    }

    val portPo = PromiseOut<Int>()
    CoroutineScope(ioAsyncExceptionHandler).launch {
      server = embeddedServer(getKtorServerEngine(), port = bindingPort) {
        install(createApplicationPlugin("dweb") {
          onCall { call ->
            withContext(ioAsyncExceptionHandler) {
              /// 将 ktor的request 构建成 pureRequest
              call.request.asPureRequest().also { rawRequest ->
                val rawUrl = rawRequest.href
                val host = findDwebGateway(rawRequest)
                val url = if (rawUrl.startsWith("/") && host !== null) {
                  "${if (rawRequest.isWebSocket()) "ws" else "http"}://$host$rawUrl"
                } else rawUrl
                val request = rawRequest.copy(href = url);

                val proxyRequestBody: ReadableStream.ReadableStreamController? = null

                val response = httpHandler(request.toClient())

                if (proxyRequestBody != null) {
                  /// 如果是200响应头，那么使用WebSocket来作为双工的通讯标准进行传输
                  when (response.status.value) {
                    200 -> {
                      val res = WebSocketUpgrade(call, null) {
                        val ws = this;
                        val streamReader = response.stream().getReader("Http1Server websocket")
                        launch {
                          /// 将从客户端收到的数据，转成 200 的标准传输到 request 的 bodyStream 中
                          for (frame in ws.incoming) {
                            proxyRequestBody.enqueue(frame.data)
                          }
                          /// 等到双工关闭，同时也关闭读取层
                          streamReader.cancel(null)
                        }
                        /// 将从服务端收到的数据，转成 200 的标准传输到 websocket 的 frame 中
                        streamReader.consumeEachArrayRange { byteArray, _ ->
                          ws.send(Frame.Binary(true, byteArray))
                        }
                        ws.close()
                      }
                      call.respond(res)
                    }

                    else -> call.response.fromPureResponse(response).also {
                      debugHttp("WS-ERROR", response.body)
                    }
                  }
                } else {
                  call.response.fromPureResponse(response)
                }
              }
            }
          }
        })
      }.start(wait = false).also {
        portPo.resolve(bindingPort)
      }
    }
    portPo.waitPromise()
  }
}
