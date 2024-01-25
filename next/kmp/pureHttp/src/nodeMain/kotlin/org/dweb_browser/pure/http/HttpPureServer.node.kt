package org.dweb_browser.pure.http

import io.ktor.http.HttpStatusCode
import js.objects.jso
import js.typedarrays.asInt8Array
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import node.buffer.Buffer
import node.http.IncomingMessage
import node.http.ServerEvent
import node.http.ServerOptions
import node.http.createServer
import node.net.Socket
import npm.ws.WebSocket.WebSocket
import npm.ws.WebSocket.WebSocketServer
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.ioAsyncExceptionHandler
import npm.ws.WebSocket.ServerOptions as WebSocketServerOptions

actual class HttpPureServer actual constructor(actual val onRequest: HttpPureServerOnRequest) {
  init {
    allHttpPureServerInstances.add(this)
  }

  private val ioScope = CoroutineScope(ioAsyncExceptionHandler)

  private val nodeHttpServer = createServer(jso<ServerOptions<*, *>> {
    requestTimeout = 30000.0
    keepAlive = true
  }) { req, res ->
    ioScope.launch {
      val pureRequest = req.asPureRequest("127.0.0.1:${portDeferred.await()}")
      val pureResponse = onRequest(pureRequest) ?: PureResponse(HttpStatusCode.GatewayTimeout)

      res.writeStatus(pureResponse.status)
      when (val pureBody = pureResponse.body) {
        is PureBinaryBody -> res.end(pureBody.data.asInt8Array())
        is PureEmptyBody -> res.end()
        is PureStreamBody -> {
          val reader = pureBody.stream.getReader("Write node.http.OutgoingMessage")
          runCatching {
            reader.consumeEachArrayRange { byteArray, _ ->
              res.write(byteArray.asInt8Array())
            }
            res.end()
          }.onFailure {
            res.writeStatus(HttpStatusCode.InternalServerError, it.message)
            res.end()
          }
        }

        is PureStringBody -> res.end(pureBody.data)
      }

    }
  }

  private val portDeferred = CompletableDeferred<UShort>()

  private val nodeWsServer = WebSocketServer(
    options = jso<WebSocketServerOptions<WebSocket, IncomingMessage>> {
      noServer = true
    }
  ).also { nodeWsServer ->
    nodeHttpServer.on(ServerEvent.UPGRADE) { req, socket, head ->
      ioScope.launch {
        val pureChannelDeferred = CompletableDeferred<PureChannel>()
        val pureRequest =
          req.asPureRequest("127.0.0.1:${portDeferred.await()}", pureChannelDeferred)
        val pureResponse = onRequest(pureRequest) ?: PureResponse(HttpStatusCode.WS_BAD_GATEWAY)
        when (pureResponse.status.value) {
          /// 如果是200响应头，说明是传统的响应模式，这时候只处理输出，websocket的incoming数据完全忽视
          200 -> {
            nodeWsServer.handleUpgrade(req, socket, head) { client, request ->
              ioScope.launch {
                val ws = client;
                val streamReader = pureResponse.stream().getReader("Http1Server websocket")
                /// 将从服务端收到的数据，转成 200 的标准传输到 websocket 的 frame 中
                launch {
                  streamReader.consumeEachArrayRange { byteArray, _ ->
                    ws.send(
                      byteArray/*, jso<npm.ws.WebSocket.SendOptions> {
                      binary = true
                    }*/
                    )
                  }
                  ws.close()
                }
                /// 忽视客户端收到的数据
                ws.on("message") { _, data, isBinary: Boolean ->

                }
                ws.on("close") { _, code: Number, reason: Buffer ->
                  /// 等到双工关闭，同时也关闭读取层
                  streamReader.cancel(null)
                }
              }
            }
          }
          /// 如果是100响应头，说明走的是标准的PureDuplex模型，那么使用WebSocket来作为双工的通讯标准进行传输
          101 -> {
            nodeWsServer.handleUpgrade(req, socket, head) { client, request ->
              val ws = client;
              val income = Channel<PureFrame>()
              val outgoing = Channel<PureFrame>()
              val pureChannel = PureChannel(income, outgoing, ws)
              pureChannelDeferred.complete(pureChannel)
              ioScope.launch {
                pipeToPureChannel(ws, pureRequest.href, income, outgoing, pureChannel)
              }
            }
          }

          else -> {
            val errorInfo =
              "HTTP/1.1 ${pureResponse.status.value} ${pureResponse.status.description}\r\n\r\n"
            (socket as Socket).apply {
              write(errorInfo);
              end()
            }
          }
        }
      }
    }
  }

  actual suspend fun start(port: UShort): UShort {
    nodeHttpServer.listen(port = port.toDouble()) {
      val addressPort = nodeHttpServer.address().asDynamic().port as Double
      portDeferred.complete(addressPort.toInt().toUShort())
    }
    return portDeferred.await()
  }

  actual suspend fun close() {
    portDeferred.await()
    nodeHttpServer.close()
  }
}