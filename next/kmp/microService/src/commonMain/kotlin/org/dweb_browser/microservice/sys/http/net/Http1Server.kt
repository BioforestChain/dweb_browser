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
import org.dweb_browser.helper.readByteArray
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
      bindingPort = 22206//ktor_ws_server.environment.config.port
      server = embeddedServer(io.ktor.server.cio.CIO, port = bindingPort) {
        install(WebSockets)
        install(createApplicationPlugin("dweb") {
          onCall { call ->
            withContext(ioAsyncExceptionHandler) {




              /// 将 ktor的request 构建成 http4k 的request
              call.request.asHttp4k()?.also { rawRequest ->
                val uri = rawRequest.uri.toString();
                val host = findRequestGateway(rawRequest)
                val url = if (uri.startsWith("/") && host !== null) {
                  "http://$host$uri"
                } else uri
                var request = rawRequest.uri(Uri.of(url));

                var proxyRequestBody: ReadableStream.ReadableStreamController? = null
                if (request.isWebSocket()) {
                  request = request.body(ReadableStream(onStart = {
                    proxyRequestBody = it
                  })).also {
                    debugHttp("WS-START", url)
                  }
                }
                val response = when (val gateway = gatewayHandler(request)) {
                  null -> errorHandler(request, null)
                  else -> httpHandler(gateway, request) ?: errorHandler(request, gateway)
                }

                if (proxyRequestBody != null) {
                  val requestBodyController = proxyRequestBody!!
                  /// 如果是200响应头，那么使用WebSocket来作为双工的通讯标准进行传输
                  when (response.status.code) {
                    200 -> {
                      val res = WebSocketUpgrade(call, null) {
                        val ws = this;
                        val stream = response.stream()
                        launch {
                          /// 将从客户端收到的数据，转成 200 的标准传输到 request 的 bodyStream 中
                          for (frame in ws.incoming) {
                            val byteArray = frame.buffer.moveToByteArray()
                            requestBodyController.enqueue(byteArray)
                          }
                          /// 等到双工关闭，同时也关闭读取层
                          stream.close()
                        }
                        /// 将从服务端收到的数据，转成 200 的标准传输到 websocket 的 frame 中
                        while (true) {
                          when (val readInt = stream.available()) {
                            -1 -> {
                              // 将会关闭
                              break
                            }

                            else -> {
                              val chunk = stream.readByteArray(readInt)
                              ws.send(Frame.Binary(true, chunk))
                            }
                          }
                        }
                        ws.close()
                      }
                      call.respond(res)
                    }

                    101 -> {
                      launch {
                        val rawRequestChannel = call.request.receiveChannel()
                        while (true) {
                          val buffer = ByteBuffer.allocate(1024)
                          when (val readSize = rawRequestChannel.readAvailable(buffer)) {
                            -1 -> {
                              requestBodyController.close()
                            }

                            else -> {
                              val byteArray = ByteArray(readSize)
                              buffer.get(byteArray)
                              requestBodyController.enqueue(byteArray)
                            }
                          }
                        }
                      }
                      call.response.fromHttp4K(response)
                    }

                    else -> call.response.fromHttp4K(response).also {
                      debugHttp("WS-ERROR", response.bodyString())
                    }
                  }
                } else {
                  call.response.fromHttp4K(response)
                }
              }
            }
          }
        })
      }.start(wait = false)

      portPo.resolve(bindingPort)
    }
    portPo.waitPromise()
  }

  val authority get() = "localhost:$bindingPort"
  val origin get() = "$PREFIX$authority"

  fun closeServer() {
    server?.also {
      it.close()
      server = null
    } ?: throw Exception("server not created")
  }
}


