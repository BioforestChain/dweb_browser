package org.dweb_browser.helper.platform.offscreenwebcanvas

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fromFilePath
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.path
import io.ktor.server.response.appendIfAbsent
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.copyAndClose
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.encodeURIComponent
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

internal class OffscreenWebCanvasMessageChannel {
  private var dataChannel = CompletableDeferred<DefaultWebSocketServerSession>()
  private var session: DefaultWebSocketServerSession? = null
  private val lock = Mutex()
  private val onMessageSignal = Signal<ChannelMessage>()
  val onMessage = onMessageSignal.toListener()
  private val client = HttpClient(CIO) {
    //install(HttpCache)
  }

  @OptIn(ExperimentalResourceApi::class)
  private val server = embeddedServer(io.ktor.server.cio.CIO, port = 23300) {
    install(WebSockets)
    routing {
      /// 图片请求的代理, 暂时只支持 get 代理
      get("/proxy") {
        val proxyUrl = call.request.queryParameters.getOrFail("url")
        client.prepareGet(proxyUrl).execute { proxyResponse ->
          call.response.headers.apply {
            for (header in proxyResponse.headers.entries()) {
              if (header.key.startsWith("Access-Control-Allow-")) {
                continue
              }
              for (value in header.value) {
                append(header.key, value)
              }
            }
            appendIfAbsent("Access-Control-Allow-Credentials", "true")
            appendIfAbsent("Access-Control-Allow-Origin", "*")
            appendIfAbsent("Access-Control-Allow-Headers", "*")
            appendIfAbsent("Access-Control-Allow-Methods", "*")
          }
          call.respondBytesWriter(status = proxyResponse.status) {
            proxyResponse.bodyAsChannel().copyAndClose(this)
          }
        }
      }
      /// 静态资源请求
      get(Regex(".+")) {
        val pathname = call.request.path()
        try {
          val content = resource("offscreen-web-canvas$pathname").readBytes()
          call.respondBytes(
            content, ContentType.fromFilePath(pathname).firstOrNull(), HttpStatusCode.fromValue(200)
          )
        } catch (e: Throwable) {
          call.respond(HttpStatusCode.fromValue(404), e.message ?: "No Found:$pathname")
        }
      }
      webSocket("/channel") {
        lock.withLock {
          if (session != null) {
            freeSession()
          }
          session = this
          dataChannel.complete(this)
        }

        for (frame in incoming) {
          frame as? Frame.Text ?: continue
          onMessageSignal.emit(ChannelMessage(frame.readText()))
        }

        lock.withLock {
          if (session == this) {
            freeSession()
          }
        }
      }
    }
  }.start(wait = false)

  suspend fun getEntryUrl(width: Int, height: Int): String {
    val port = server.resolvedConnectors().first().port
    val host = "localhost"
    val entry = "http://$host:$port/index.html"//"http://172.30.92.50:5173/index.html"//
    return "$entry?width=$width&height=$width&channel=${"ws://$host:$port/channel".encodeURIComponent()}&proxy=${"http://$host:$port/proxy".encodeURIComponent()}"
  }

//  suspend fun postMessage(message: ByteArray) {
//    val session = this.session ?: dataChannel.await()
//    session.send(message)
//  }

  suspend fun postMessage(message: String) {
    val session = this.session ?: dataChannel.await()
    session.send(message)
  }

  suspend fun close() {
    val session = this.session ?: dataChannel.await()
    lock.withLock {
      session.close()
      freeSession()
    }
  }

  private fun freeSession() {
    session = null
    dataChannel = CompletableDeferred()
  }

}