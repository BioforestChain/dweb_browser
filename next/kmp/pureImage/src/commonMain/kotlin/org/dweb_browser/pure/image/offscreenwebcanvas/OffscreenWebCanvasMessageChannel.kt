package org.dweb_browser.pure.image.offscreenwebcanvas

import dweb_browser_kmp.pureimage.generated.resources.Res
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fromFilePath
import io.ktor.http.hostWithPort
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.mapNotNull
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
import org.jetbrains.compose.resources.ExperimentalResourceApi

internal suspend fun commonStartPureServer(server: HttpPureServer) = server.start(0u)
internal expect suspend fun startPureServer(server: HttpPureServer): UShort
internal class OffscreenWebCanvasMessageChannel {
  private var dataChannel = CompletableDeferred<PureChannel>()
  private var session: PureChannel? = null
  private val lock = Mutex()
  private val onMessageSignal = Signal<ChannelMessage>()
  val onMessage = onMessageSignal.toListener()
  val proxy = OffscreenWebCanvasFetchProxy()

  @OptIn(ExperimentalResourceApi::class)
  private val server = HttpPureServer {
    if (it.url.hostWithPort != hostWithPort) {
      return@HttpPureServer null
    }
    val pathname = it.url.encodedPath
    if (pathname == "/channel" && it.hasChannel) {
      it.byChannel {
        val pureChannel = this
        val dataChannel = this@OffscreenWebCanvasMessageChannel.dataChannel
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
              is PureBinaryFrame -> onMessageSignal.emit(ChannelMessage(binary = frame.binary))
              is PureTextFrame -> onMessageSignal.emit(ChannelMessage(text = frame.text))
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
        val content = Res.readBytes("files/offscreen-web-canvas$pathname")
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

  private var hostWithPort = ""
  val entryUrlFlow = server.stateFlow.mapNotNull { port ->
    when (port) {
      null -> {
        hostWithPort = ""
        startPureServer(server)
        null
      }

      else -> {
        hostWithPort = "localhost:$port"
        val entry = "http://$hostWithPort/index.html"
        "$entry?channel=${"ws://$hostWithPort/channel".encodeURIComponent()}&proxy=${"http://$hostWithPort/proxy".encodeURIComponent()}"
      }
    }
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
    session.start().sendText(message)
  }

  private fun freeSession() {
    session = null
    dataChannel = CompletableDeferred()
  }
}