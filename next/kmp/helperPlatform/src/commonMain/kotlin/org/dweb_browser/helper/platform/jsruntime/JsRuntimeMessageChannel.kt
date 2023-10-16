package org.dweb_browser.helper.platform.jsruntime

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fromFilePath
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.consumeEachByteArrayPacket
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.getKtorServerEngine
import org.dweb_browser.helper.toLittleEndianByteArray
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

class ReqResChannel(val input: ByteReadChannel) {
  val output = ByteChannel(true)
  val onMessage = channelFlow {
    try {
      input.consumeEachByteArrayPacket {
        send(it)
      }
      close()
      this@ReqResChannel.close()
    } catch (e: Throwable) {
      close(e)
      input.cancel(e);
      this@ReqResChannel.close(e)
    }
  }
  private val onCloseDeferred = CompletableDeferred<Unit>()
  val onClose = flow<Throwable?> {
    try {
      onCloseDeferred.await()
      emit(null)
    } catch (e: Throwable) {
      emit(e)
    } finally {
      close()
    }
  }

  suspend fun send(data: ByteArray) {
    when (val size = data.size) {
      0 -> {}
      else -> output.writePacket(ByteReadPacket(size.toLittleEndianByteArray() + data))
    }
  }

  fun close(cause: Throwable? = null) {
    if (onCloseDeferred.isActive) {
      input.cancel(cause)
      output.close(cause)
      onCloseDeferred.complete(Unit)
    }
  }
}

class SuperChannel() {
  class SessionContext(private val session: DefaultWebSocketServerSession) {
    val messageLock = Mutex()

    private val LOCK_SIZE = 1024 * 1024 * 10// 10mb 锁一次
    private var currentSize = 0;
    private suspend fun tryLock(size: Int) {
      currentSize += size
      if (currentSize > LOCK_SIZE) {
        session.send("🔒")
        messageLock.lock()
      }
    }

    suspend fun postMessage(message: String) {
      tryLock(message.length)
      session.send(message)
    }

    suspend fun postMessage(message: ByteArray) {
      tryLock(message.size)
      session.send(message)
    }

    suspend fun close(reason: CloseReason? = null) {
      when (reason) {
        null -> session.close()
        else -> session.close(reason)
      }
    }
  }

  private val contexts = mutableListOf<SessionContext>()

  constructor(session: DefaultWebSocketServerSession) : this() {
    setupSession(session)
  }

  private val scope = CoroutineScope(ioAsyncExceptionHandler)
  private val onMessageSignal = Signal<ChannelMessage>()
  val onMessage = onMessageSignal.toListener()
  private val sessionsLock = Mutex()
  suspend fun addSession(session: DefaultWebSocketServerSession) = sessionsLock.withLock {
    setupSession(session)
  }

  private fun setupSession(session: DefaultWebSocketServerSession) {
    val context = SessionContext(session)
    contexts.add(context)
    scope.launch {
      for (frame in session.incoming) {
        when (frame) {
          is Frame.Text -> {
            val text = frame.readText()
            if (text == "🔓") {
              context.messageLock.unlock()
            } else {
              onMessageSignal.emit(ChannelMessage(text = text))
            }
          }

          is Frame.Binary -> onMessageSignal.emit(ChannelMessage(binary = frame.data))
          else -> {}
        }
      }
    }
  }

  private val onCloseDeferred = CompletableDeferred<Unit>()
  val onClose = flow<Throwable?> {
    try {
      onCloseDeferred.await()
      emit(null)
    } catch (e: Throwable) {
      emit(e)
    } finally {
      close()
    }
  }

  suspend fun close(reason: CloseReason? = null) {
    for (context in contexts) {
      context.close(reason)
    }
    contexts.clear()
  }

  private var it = 0
  private suspend fun pickContext() = sessionsLock.withLock {
    contexts[it++ % contexts.size]
  }

  suspend fun postMessage(msg: String) {
    val context = pickContext();
    context.postMessage(msg)
  }

  suspend fun postMessage(msg: ByteArray) {
    val context = pickContext();
    context.postMessage(msg)
  }
}

class JsRuntimeMessageChannel {
  private var dataChannel = CompletableDeferred<DefaultWebSocketServerSession>()
  private var session: DefaultWebSocketServerSession? = null
  private val channelLock = Mutex()
  private val reqResChannelDeferred = CompletableDeferred<ReqResChannel>()
  suspend fun getReqResChannel() = reqResChannelDeferred.await()
  private val superChannelLock = Mutex()
  private val superChannelDeferred = CompletableDeferred<SuperChannel>()
  suspend fun getSuperChannel() = superChannelDeferred.await()
  private val onMessageSignal = Signal<ChannelMessage>()
  val onMessage = onMessageSignal.toListener()

  private val messageLock = Mutex()


  @OptIn(ExperimentalResourceApi::class)
  private val server = embeddedServer(getKtorServerEngine(), port = 0) {
    install(WebSockets)
    routing {
      post("/c2s-channel") {
        val reqResChannel = ReqResChannel(call.request.receiveChannel())
        reqResChannelDeferred.complete(reqResChannel)
        call.respondBytesWriter {
          val cause = reqResChannel.onClose.first()
          close(cause)
        }
      }
      get("/s2c-channel") {
        call.respondBytesWriter { getReqResChannel().output.copyTo(this) }
      }
      /// 静态资源请求
      get(Regex(".+")) {
        val pathname = call.request.path()
        try {
          val content = resource("js-runtime$pathname").readBytes()
          call.respondBytes(
            content, ContentType.fromFilePath(pathname).firstOrNull(), HttpStatusCode.fromValue(200)
          )
        } catch (e: Throwable) {
          call.respond(HttpStatusCode.fromValue(404), e.message ?: "No Found:$pathname")
        }
      }
      webSocket("/super-channel") {
        superChannelLock.withLock {
          if (!superChannelDeferred.isCompleted) {
            superChannelDeferred.complete(SuperChannel(this))
          } else {
            val superChannel = superChannelDeferred.await()
            superChannel.addSession(this)
          }
        }
        val superChannel = superChannelDeferred.await()
        superChannel.onClose.first()
      }
      webSocket("/channel") {
        channelLock.withLock {
          if (session != null) {
            freeSession()
          }
          session = this
          dataChannel.complete(this)
        }

        for (frame in incoming) {
          when (frame) {
            is Frame.Text -> {
              val text = frame.readText()
              if (text == "🔓") {
                messageLock.unlock()
              } else {
                onMessageSignal.emit(ChannelMessage(text = text))
              }
            }

            is Frame.Binary -> onMessageSignal.emit(ChannelMessage(binary = frame.data))
            else -> {}
          }
        }

        channelLock.withLock {
          if (session == this) {
            freeSession()
          }
        }
      }
    }
  }.start(wait = false)

  suspend fun getEntryUrl(): String {
    val port = server.resolvedConnectors().first().port
    val host = "localhost"
    val entry = "http://$host:$port/index.html"//"http://172.30.92.50:5173/index.html"//
    return "$entry?channel=${"ws://$host:$port/channel".encodeURIComponent()}"
  }

  //  suspend fun postMessage(message: ByteArray) {
//    val session = this.session ?: dataChannel.await()
//    session.send(message)
//  }
  suspend fun waitReady() {
    this.session ?: dataChannel.await()
  }

  private val LOCK_SIZE = 1024 * 1024 * 10// 10mb 锁一次
  private var currentSize = 0;
  private suspend fun tryLock(size: Int) {
    currentSize += size
    if (currentSize > LOCK_SIZE) {
      this.session!!.send("🔒")
      messageLock.lock()
    }
  }

  suspend fun postMessage(message: String) {
    val session = this.session ?: dataChannel.await()
    tryLock(message.length)
    session.send(message)
  }

  suspend fun postMessage(message: ByteArray) {
    val session = this.session ?: dataChannel.await()
    tryLock(message.size)
    session.send(message)
  }

  suspend fun close() {
    val session = this.session ?: dataChannel.await()
    channelLock.withLock {
      session.close()
      freeSession()
    }
  }

  private fun freeSession() {
    session = null
    dataChannel = CompletableDeferred()
  }

}