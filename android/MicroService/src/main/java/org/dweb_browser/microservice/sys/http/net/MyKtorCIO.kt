package org.dweb_browser.microservice.sys.http.net

import io.ktor.http.*
import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.withContext
import org.http4k.core.*
import org.http4k.core.Headers
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.server.Http4kServer
import org.http4k.server.PolyServerConfig
import org.http4k.server.ServerConfig
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.server.asHttp4k
import org.http4k.server.supportedOrNull
import org.http4k.sse.SseHandler
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import org.java_websocket.WebSocket
import org.java_websocket.drafts.Draft
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import io.ktor.http.Headers as KHeaders

@Suppress("EXPERIMENTAL_API_USAGE")
class MyKtorCIO(val port: Int = 22206, override val stopMode: ServerConfig.StopMode) :
  PolyServerConfig {
  private val drafts: List<Draft>? = null
  private val hostName: String = "localhost"
  private val configFn: WebSocketServer.() -> Unit = {
    isReuseAddr = true // Set SO_REUSEADDR by default
  }

  constructor(port: Int = 22206) : this(port, Immediate)

  init {
    if (stopMode != Immediate) {
      throw ServerConfig.UnsupportedStopMode(stopMode)
    }
  }

  override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?): Http4kServer  {

    if (http == null) {
      throw Exception("http Handler is null!")
    }
    if (ws == null) {
      throw Exception("websocket Handler is null!")
    }
    val startLatch = CountDownLatch(1)
    val address = InetSocketAddress(hostName, port)
    val server = createServer(ws, address, drafts, startLatch::countDown).also(configFn)


    val http4kServer = object : Http4kServer {
      override fun port() = engine.environment.connectors[0].port
      override fun start() = also {
        engine.start()
        server.start()
      }

      override fun stop() = also {
        when (stopMode) {
          Immediate -> {
            engine.stop(0, 2, SECONDS)
            server.stop()
          }
          is ServerConfig.StopMode.Graceful -> server.stop(stopMode.timeout.toMillis().toInt())
        }
      }

      private val engine: CIOApplicationEngine = embeddedServer(CIO, port) {
        install(createApplicationPlugin(name = "http4k") {
          onCall {
            withContext(Default) {
              if (it.request.headers[HttpHeaders.Upgrade]?.equals("websocket",true) == true) {
               it.request.asHttp4k()?.let(ws)
              } else {
                it.response.fromHttp4K(
                  it.request.asHttp4k()?.let(http) ?: Response(
                    NOT_IMPLEMENTED
                  )
                )
              }
            }
          }
        })
      }
    }
    return http4kServer
  }
}

fun ApplicationRequest.asHttp4k() = Method.supportedOrNull(httpMethod.value)?.let {
  Request(it, uri)
    .headers(headers.toHttp4kHeaders())
    .body(receiveChannel().toInputStream(), header("Content-Length")?.toLong())
    .source(
      RequestSource(
        origin.remoteHost,
        scheme = origin.scheme
      )
    ) // origin.remotePort does not exist for Ktor
}

suspend fun ApplicationResponse.fromHttp4K(response: Response) {
  status(HttpStatusCode.fromValue(response.status.code))
  response.headers
    .filterNot { HttpHeaders.isUnsafe(it.first) || it.first == CONTENT_TYPE.meta.name }
    .forEach { header(it.first, it.second ?: "") }
  call.respondOutputStream(
    CONTENT_TYPE(response)?.let { ContentType.parse(it.toHeaderValue()) }
  ) {
    response.body.stream.copyToWithFlush(this)
  }
}

private fun InputStream.copyToWithFlush(
  out: OutputStream,
  bufferSize: Int = DEFAULT_BUFFER_SIZE
): Long {
  var bytesCopied: Long = 0
  val buffer = ByteArray(bufferSize)
  try {
    var bytes = read(buffer)
    while (bytes >= 0) {
      out.write(buffer, 0, bytes)
      out.flush()
      bytesCopied += bytes
      bytes = read(buffer)
    }
  } catch (e: Exception) {
    close()
    throw e
  }
  return bytesCopied
}

private fun KHeaders.toHttp4kHeaders(): Headers = names().flatMap { name ->
  (getAll(name) ?: emptyList()).map { name to it }
}

private fun createServer(
  wsHandler: WsHandler,
  address: InetSocketAddress,
  drafts: List<Draft>?,
  onServerStart: () -> Unit
) = object : WebSocketServer(address, drafts) {

  override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
    val headers = handshake.iterateHttpFields()
      .asSequence()
      .map { it to handshake.getFieldValue(it) }
      .toList()

    val upgradeRequest = Request(Method.GET, handshake.resourceDescriptor)
      .headers(headers)
      .let { if (handshake.content != null) it.body(MemoryBody(handshake.content)) else it }
      .source(RequestSource(conn.remoteSocketAddress.hostString, conn.remoteSocketAddress.port))

    val wsAdapter = object : PushPullAdaptingWebSocket() {
      override fun send(message: WsMessage) {
        when (message.body) {
          is StreamBody -> conn.send(message.body.payload)
          else -> conn.send(message.bodyString())  // furthering the generalization that a MemoryBody is ALWAYS to use text mode
        }
      }

      override fun close(status: WsStatus) {
        conn.close(status.code, status.description)
      }
    }

    conn.setAttachment(wsAdapter)
    wsHandler(upgradeRequest)(wsAdapter)
  }

  override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
    conn.adapter()?.triggerClose(WsStatus(code, reason ?: ""))
  }

  override fun onMessage(conn: WebSocket, message: ByteBuffer) {
    conn.adapter()?.let { ws ->
      try {
        ws.triggerMessage(WsMessage(MemoryBody(message)))
      } catch (e: Throwable) {
        ws.triggerError(e)
      }
    }
  }

  override fun onMessage(conn: WebSocket, message: String) {
    conn.adapter()?.let { ws ->
      try {
        ws.triggerMessage(WsMessage(message))
      } catch (e: Throwable) {
        ws.triggerError(e)
      }
    }
  }

  override fun onError(conn: WebSocket?, ex: Exception) {
    conn?.adapter()?.triggerError(ex)
  }

  override fun onStart() = onServerStart()
}

private fun WebSocket.adapter(): PushPullAdaptingWebSocket? = getAttachment()
