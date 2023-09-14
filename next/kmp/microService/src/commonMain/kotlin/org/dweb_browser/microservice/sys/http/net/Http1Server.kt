package org.dweb_browser.microservice.sys.http.net

import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSocketUpgrade
import io.ktor.server.websocket.WebSockets
import io.ktor.util.InternalAPI
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.getKtorServerEngine
import org.dweb_browser.microservice.help.asPureRequest
import org.dweb_browser.microservice.help.consumeEachArrayRange
import org.dweb_browser.microservice.help.fromPureResponse
import org.dweb_browser.microservice.help.isWebSocket
import org.dweb_browser.microservice.http.PureRequest
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import org.dweb_browser.microservice.sys.http.Gateway
import org.dweb_browser.microservice.sys.http.debugHttp
import org.dweb_browser.microservice.sys.http.findRequestGateway

typealias GatewayHandler = suspend (request: PureRequest) -> Gateway?
typealias GatewayHttpHandler = suspend (gateway: Gateway, request: PureRequest) -> PureResponse?
typealias GatewayErrorHandler = suspend (request: PureRequest, gateway: Gateway?) -> PureResponse


class Http1Server {
  companion object {
    const val PREFIX = "http://";
    const val PROTOCOL = "http:";
    const val PORT = 80;
  }

  private var bindingPort = -1

  private var server: ApplicationEngine? = null

  @OptIn(InternalAPI::class)
  suspend fun createServer(
    gatewayHandler: GatewayHandler,
    httpHandler: GatewayHttpHandler,
    errorHandler: GatewayErrorHandler
  ) {
    if (server != null) {
      throw Exception("server alter created")
    }

    val portPo = PromiseOut<Int>()
    CoroutineScope(ioAsyncExceptionHandler).launch {
      server = embeddedServer(getKtorServerEngine(), port = 0) {
        install(WebSockets)
        install(createApplicationPlugin("dweb") {
          onCall { call ->
            withContext(ioAsyncExceptionHandler) {
              /// 将 ktor的request 构建成 pureRequest
              call.request.asPureRequest().also { rawRequest ->
                val rawUrl = rawRequest.url
                val host = findRequestGateway(rawRequest)
                val url = if (rawUrl.startsWith("/") && host !== null) {
                  "http://$host$rawUrl"
                } else rawUrl
                var request = rawRequest.copy(url = url);

                var proxyRequestBody: ReadableStream.ReadableStreamController? = null
                if (request.isWebSocket()) {
                  request = request.copy(body = (ReadableStream(onStart = {
                    proxyRequestBody = it
                  })).also {
                    debugHttp("WS-START", url)
                  }.stream.toBody())
                }
                val response = when (val gateway = gatewayHandler(request)) {
                  null -> errorHandler(request, null)
                  else -> httpHandler(gateway, request) ?: errorHandler(request, gateway)
                }

                if (proxyRequestBody != null) {
                  val requestBodyController = proxyRequestBody!!
                  /// 如果是200响应头，那么使用WebSocket来作为双工的通讯标准进行传输
                  when (response.status.value) {
                    200 -> {
                      val res = WebSocketUpgrade(call, null) {
                        val ws = this;
                        val streamReader = response.stream().getReader("Http1Server websocket")
                        launch {
                          /// 将从客户端收到的数据，转成 200 的标准传输到 request 的 bodyStream 中
                          for (frame in ws.incoming) {
                            requestBodyController.enqueue(frame.data)
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

                    101 -> {
                      launch {
                        val rawRequestChannel = call.request.receiveChannel()
                        rawRequestChannel.consumeEachArrayRange { byteArray, _ ->
                          requestBodyController.enqueue(byteArray)
                        }
                        requestBodyController.close()
                      }
                      call.response.fromPureResponse(response)
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
        val bindingPort = it.resolvedConnectors().first().port
        portPo.resolve(bindingPort)
      }
    }
    portPo.waitPromise()
  }

  val authority get() = "localhost:$bindingPort"
  val origin get() = "$PREFIX$authority"

  fun closeServer() {
    server?.also {
      it.stop()
      server = null
    } ?: throw Exception("server not created")
  }
}


