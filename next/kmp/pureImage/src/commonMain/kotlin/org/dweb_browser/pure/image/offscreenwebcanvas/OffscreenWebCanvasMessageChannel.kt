package org.dweb_browser.pure.image.offscreenwebcanvas

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fromFilePath
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.pure.http.HttpPureServer
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureBinaryFrame
import org.dweb_browser.pure.http.PureChannel
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureTextFrame
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes

internal class OffscreenWebCanvasMessageChannel {
  private var dataChannel = CompletableDeferred<PureChannel>()
  private var session: PureChannel? = null
  private val lock = Mutex()
  private val onMessageSignal = Signal<ChannelMessage>()
  val onMessage = onMessageSignal.toListener()
  val proxy = OffscreenWebCanvasFetchProxy()

  @OptIn(InternalResourceApi::class)
  private val server = HttpPureServer {
    val pathname = it.url.encodedPath
    if (pathname == "/channel" && it.hasChannel) {
      it.byChannel {
        val pureChannel = this
        lock.withLock {
          if (session != null) {
            freeSession()
          }
          session = pureChannel
          dataChannel.complete(pureChannel)
        }
        pureChannel.start().apply {
          for (frame in income) {
            if (session != pureChannel) {
              return@apply
            }
            when (frame) {
              is PureBinaryFrame -> onMessageSignal.emit(ChannelMessage(binary = frame.data))
              is PureTextFrame -> onMessageSignal.emit(ChannelMessage(text = frame.data))
            }
          }
        }

        lock.withLock {
          if (session == pureChannel) {
            freeSession()
          }
        }
      }
    } else if (pathname == "/proxy") {
      proxy.proxy(it)
    } else {
      runCatching {
        val content = readResourceBytes("offscreen-web-canvas$pathname")
        PureResponse(body = IPureBody.from(content), headers = PureHeaders().apply {
          val extension = ContentType.fromFilePath(pathname)
          extension.firstOrNull()?.apply {
            init("Content-Type", toString())
          }
        })
      }.getOrElse {
        PureResponse(
          HttpStatusCode.NotFound,
          body = IPureBody.from(it.message ?: "No Found:$pathname")
        )
      }
    }
  }

  suspend fun getEntryUrl(width: Int, height: Int): String {
    val port = server.start(0u)
    val host = "localhost"
    val entry = "http://$host:$port/index.html"
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
    session.afterStart().sendText(message)
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