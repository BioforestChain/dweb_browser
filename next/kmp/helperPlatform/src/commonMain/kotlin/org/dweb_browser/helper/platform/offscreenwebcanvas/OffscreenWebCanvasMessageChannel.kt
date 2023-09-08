package org.dweb_browser.helper.platform.offscreenwebcanvas

import io.ktor.client.HttpClient
import io.ktor.client.request.header
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
import io.ktor.util.flattenEntries
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
import org.dweb_browser.helper.platform.getKtorClientEngine
import org.dweb_browser.helper.platform.getKtorServerEngine
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

internal class OffscreenWebCanvasMessageChannel {
  private var dataChannel = CompletableDeferred<DefaultWebSocketServerSession>()
  private var session: DefaultWebSocketServerSession? = null
  private val lock = Mutex()
  private val onMessageSignal = Signal<ChannelMessage>()
  val onMessage = onMessageSignal.toListener()
  private val client = HttpClient(getKtorClientEngine()) {
    //install(HttpCache)
  }

  @OptIn(ExperimentalResourceApi::class)
  private val server = embeddedServer(getKtorServerEngine(), port = 0) {
    install(WebSockets)
    routing {
      /// 图片请求的代理, 暂时只支持 get 代理
      get("/proxy") {
        val proxyUrl = call.request.queryParameters.getOrFail("url")
        client.prepareGet(proxyUrl) {
          for ((key, value) in call.request.headers.flattenEntries()) {
            /// 把访问源头过滤掉，未来甚至可能需要额外加上，避免同源限制，但具体如何去加，跟对方的服务器有关系，很难有标准答案，所以这里索性直接移除了
            if (key == "Referer" || key == "Origin" || key == "Host") {
              continue
            }
            header(key, value)
          }
        }.execute { proxyResponse ->
          call.response.headers.apply {
            for ((key, value) in proxyResponse.headers.flattenEntries()) {
              // 这里过滤掉 访问控制相关的配置，重写成全部开放的模式
              if (key.startsWith("Access-Control-Allow-") ||
                // 跟内容编码与长度有关的，也全部关掉，proxyResponse.bodyAsChannel 的时候，得到的其实是解码过的内容，所以这些内容编码与长度的信息已经不可用了
                key == "Content-Encoding" || key == "Content-Length"
              ) {
                continue
              }
              append(key, value)
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
    val entry = "http://172.30.92.50:5173/index.html"// "http://$host:$port/index.html"//
    return "$entry?width=$width&height=$height&channel=${"ws://$host:$port/channel".encodeURIComponent()}&proxy=${"http://$host:$port/proxy".encodeURIComponent()}"
  }

  //  suspend fun postMessage(message: ByteArray) {
//    val session = this.session ?: dataChannel.await()
//    session.send(message)
//  }
  suspend fun waitReady() {
    this.session ?: dataChannel.await()
  }

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