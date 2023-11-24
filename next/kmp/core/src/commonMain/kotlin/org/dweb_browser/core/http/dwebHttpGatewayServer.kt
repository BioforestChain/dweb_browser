package org.dweb_browser.core.http


import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSocketUpgrade
import io.ktor.server.websocket.WebSockets
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.core.help.asPureRequest
import org.dweb_browser.core.help.fromPureResponse
import org.dweb_browser.core.help.isWebSocket
import org.dweb_browser.core.ipc.helper.ReadableStream
import org.dweb_browser.core.std.http.debugHttp
import org.dweb_browser.core.std.http.findDwebGateway
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.getKtorServerEngine


typealias HttpGateway = suspend (request: PureRequest) -> PureResponse?

class DwebGatewayHandlerAdapterManager : AdapterManager<HttpGateway>() {
  suspend fun doGateway(request: PureRequest): PureResponse? {
    for (adapter in adapters) {
      val response = adapter(request)
      if (response != null) {
        return response
      }
    }
    return null
  }
}

class DwebHttpGatewayServer private constructor() {
  companion object {
    val INSTANCE by lazy { DwebHttpGatewayServer() }
    val gatewayAdapterManager = DwebGatewayHandlerAdapterManager()
  }

  val server = embeddedServer(getKtorServerEngine(), port = 0) {
    install(WebSockets)
    install(createApplicationPlugin("dweb-gateway") {
      onCall { call ->
        withContext(ioAsyncExceptionHandler) {
          /// 将 ktor的request 构建成 pureRequest
          call.request.asPureRequest().also { rawRequest ->
            val rawUrl = rawRequest.href
            val url = when (val info = findDwebGateway(rawRequest)) {
              null -> rawUrl
              else -> "${info.protocol.name}://${info.host}$rawUrl"
            }
            var request = if (url != rawUrl) rawRequest.copy(href = url) else rawRequest;

            var proxyRequestBody: ReadableStream.ReadableStreamController? = null
            if (request.isWebSocket()) {
              request = request.copy(body = (ReadableStream {
                proxyRequestBody = it
              }).also {
                debugHttp("WS-START", url)
              }.stream.toBody())
            }
            val response = try {
              gatewayAdapterManager.doGateway(request)
                ?: PureResponse(HttpStatusCode.GatewayTimeout)
            } catch (e: Throwable) {
              PureResponse(HttpStatusCode.BadGateway, body = IPureBody.from(e.message ?: ""))
            }

            if (proxyRequestBody != null) {
              val requestBodyController = proxyRequestBody!!
              /// 如果是200响应头，那么使用WebSocket来作为双工的通讯标准进行传输
              when (response.status.value) {
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
                    /// 将从客户端收到的数据，转成 200 的标准传输到 request 的 bodyStream 中
                    for (frame in ws.incoming) {// 注意，这里ws.incoming要立刻进行，不能在launch中异步执行，否则ws将无法完成连接建立
                      requestBodyController.enqueue(frame.data)
                    }
                    /// 等到双工关闭，同时也关闭读取层
                    streamReader.cancel(null)

                  }
                  call.respond(res)
                }

                101 -> {
                  launch {
                    val rawRequestChannel = call.request.receiveChannel()
                    rawRequestChannel.consumeEachArrayRange { byteArray, _ ->
                      requestBodyController.enqueue(byteArray)
                    }
                    requestBodyController.closeWrite()
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

  }
  private val startResult = lazy {
    CompletableDeferred<Int>()
  }

  suspend fun startServer(): Int {
    if (!startResult.isInitialized()) {
      startResult.value.also {
        server.start(wait = false)
        val port = server.resolvedConnectors().first().port
        it.complete(port)
      }
    }
    return startResult.value.await()
  }

  suspend fun getPort() = startServer()
  val getHttpLocalhostGatewaySuffix = SuspendOnce { ".localhost:${startServer()}" }

  suspend fun getUrl() = "http://127.0.0.1:${startServer()}"

}

val dwebHttpGatewayServer get() = DwebHttpGatewayServer.INSTANCE