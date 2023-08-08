package org.dweb_browser.microservice.sys.http.net

import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.InternalAPI
import io.ktor.util.moveToByteArray
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dweb_browser.helper.*
import org.dweb_browser.microservice.help.stream
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import org.dweb_browser.microservice.sys.http.Gateway
import org.dweb_browser.microservice.sys.http.debugHttp
import org.http4k.core.*
import org.http4k.server.Http4kServer
import java.nio.ByteBuffer

typealias GatewayHandler = suspend (request: Request) -> Gateway?
typealias GatewayHttpHandler = suspend (gateway: Gateway, request: Request) -> Response?
typealias GatewayErrorHandler = suspend (request: Request, gateway: Gateway?) -> Response


class Http1Server {
  companion object {
    const val PREFIX = "http://";
    const val PROTOCOL = "http:";
    const val PORT = 80;
  }

  private var bindingPort = -1

  private var server: Http4kServer? = null

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
      launch {
        val ktor_ws_server = embeddedServer(io.ktor.server.cio.CIO, port = 22206) {
          install(WebSockets)
          install(createApplicationPlugin("dweb") {
            onCall { call ->
              withContext(ioAsyncExceptionHandler) {

                /// 将 ktor的request 构建成 http4k 的request
                call.request.asHttp4k()?.also { rawRequest ->
                  var request = rawRequest;
                  var proxyRequestBody: ReadableStream.ReadableStreamController? = null
                  if (request.isWebSocket()) {
                    request = request.body(ReadableStream(onStart = {
                      proxyRequestBody = it
                    }))
                  }
                  val response = when (val gateway = gatewayHandler(request)) {
                    null -> errorHandler(request, null)
                    else -> httpHandler(gateway, request) ?: errorHandler(request, gateway)
                  }

//                  debugHttp(
//                    "createServer",
//                    "uri:${request.uri} ws:${request.isWebSocket()} res:${response.status.code}"
//                  )
                  if (request.isWebSocket()) {
                    /// 如果是200响应头，那么使用WebSocket来作为双工的通讯标准进行传输
                    when (response.status.code) {
                      200 -> {
                        val res = WebSocketUpgrade(call, null) {
                          val ws = this;
                          val stream = response.stream()
                          launch {
                            /// 将从客户端收到的数据，转成 200 的标准传输到 request 的 bodyStream 中
                            for (frame in ws.incoming) {
                              proxyRequestBody?.enqueue(frame.buffer.moveToByteArray())
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
                                debugHttp(
                                  "createServer", "ws200 send response:${chunk}"
                                )
                                ws.send(Frame.Binary(true, chunk))
                              }
                            }
                          }
                          debugHttp(
                            "createServer", "ws200 close ws"
                          )
                          ws.close()
                        }
                        call.respond(res)
//                        call.respondBytesWriter(status = HttpStatusCode.SwitchingProtocols) {
//                          val rawWebSocket =
//                            RawWebSocket(
//                              call.request.receiveChannel(),
//                              this,
//                              Int.MAX_VALUE.toLong(),
//                              false,
//                              coroutineContext
//                            );
//                          val ws = DefaultWebSocketSession(rawWebSocket);
//                          ws.start(call.attributes[WebSockets.EXTENSIONS_KEY])
//
//                          val stream = response.stream()
//                          launch {
//                            val closedReason = ws.closeReason.await()
//                            stream.close()
//                          }
//                          while (true) {
//                            when (val readInt = stream.available()) {
//                              -1 -> {
//                                ws.close()
//                                break
//                              }
//
//                              else -> {
//                                val chunk = stream.readByteArray(readInt)
//                                ws.send(Frame.Binary(true, chunk))
//                              }
//                            }
//                          }
//
//                          ws.close()
//                        }
                      }

                      101 -> {
                        when (val requestBodyStream = proxyRequestBody) {
                          null -> {}
                          else -> launch {
                            val rawRequestChannel = call.request.receiveChannel()
                            while (true) {
                              val buffer = ByteBuffer.allocate(1024)
                              when (val readSize = rawRequestChannel.readAvailable(buffer)) {
                                -1 -> {
                                  requestBodyStream.close()
                                }

                                else -> {
                                  val byteArray = ByteArray(readSize)
                                  buffer.get(byteArray)
                                  requestBodyStream.enqueue(byteArray)
                                }
                              }
                            }
                          }
                        }

                        call.response.fromHttp4K(response)
                      }

                      else -> call.response.fromHttp4K(response)
                    }
                  } else {
                    call.response.fromHttp4K(response)
                  }
                }
              }
            }
          })
//          routing {
//            webSocket("*") {
//              val response = call.request.asHttp4k()?.let { request ->
//                when (val gateway = gatewayHandler(request)) {
//                  null -> errorHandler(request, null)
//                  else -> httpHandler(gateway, request) ?: errorHandler(request, gateway)
//                }
//              } ?: throw Exception("invalid ws request");
//
//              when (response.status.code) {
//                200 -> {}
//                101 -> {
//                  val ws = this@webSocket
////                  ws.
//                }
//                else -> close(
//                  CloseReason(
//                    CloseReason.Codes.values().find { it.code == response.status.code.toShort() }
//                      ?: CloseReason.Codes.NORMAL,
//                    response.bodyString()
//                  )
//                )
//              }
//
//              send("Please enter your name")
//              for (frame in incoming) {
//                frame as? Frame.Text ?: continue
//                val receivedText = frame.readText()
//                if (receivedText.equals("bye", ignoreCase = true)) {
//                  close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
//                } else {
//                  send(Frame.Text("Hi, $receivedText!"))
//                }
//              }
//              println("closed!!!")
//            }
//            val all: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit = {
//              call.request.asHttp4k()?.also { request ->
//                val response = when (val gateway = gatewayHandler(request)) {
//                  null -> errorHandler(request, null)
//                  else -> httpHandler(gateway, request) ?: errorHandler(request, gateway)
//                }
//                call.response.fromHttp4K(response)
//              }
//            }
//            get("*", all)
//            post("*", all)
//            put("*", all)
//            delete("*", all)
//            options("*", all)
//            head("*", all)
//            patch("*", all)
//
//          }
        }.start()

        bindingPort = 22206//ktor_ws_server.environment.config.port
        portPo.resolve(bindingPort)
      }

//      server =
//        { request: Request ->
//          runBlockingCatching {
//            when (val gateway = gatewayHandler(request)) {
//              null -> errorHandler(request, null)
//              else -> httpHandler(gateway, request) ?: errorHandler(request, gateway)
//            }
//          }.getOrThrow()
//        }.asServer(
//          org.http4k.server.Netty(
//            22206/* 使用随机端口*/,
//            ServerConfig.StopMode.Immediate
//          )
//        ).start().also { server ->
//          bindingPort = server.port()
//          portPo.resolve(bindingPort)
//        }
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


