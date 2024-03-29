package org.dweb_browser.pure.http

import io.ktor.http.HttpStatusCode
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.IFrom
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.SuspendOnce


interface IPureChannel {
  val onStart: Signal.Listener<PureChannelContext>
  suspend fun start(): PureChannelContext
  val onClose: Signal.Listener<Unit>
  suspend fun close(cause: Throwable? = null, reason: CancellationException? = null)
}

class PureChannelContext internal constructor(
  val income: Channel<PureFrame>,
  val outgoing: Channel<PureFrame>,
  private val closeSignal: SimpleSignal,
  val getChannel: () -> PureChannel
) {
  operator fun iterator() = income.iterator()
  inline suspend fun <reified T> readJsonLine() = sequenceOf(iterator())

  suspend fun sendText(data: String) =
    outgoing.send(PureTextFrame(data))

  suspend inline fun <reified T> sendJson(data: T) =
    sendText(Json.encodeToString(data))

  suspend inline fun <reified T> sendJsonLine(data: T) =
    sendText(Json.encodeToString(data) + "\n")


  suspend fun sendBinary(data: ByteArray) =
    outgoing.send(PureBinaryFrame(data))


  @OptIn(ExperimentalSerializationApi::class)
  suspend inline fun <reified T> sendCbor(data: T) =
    sendBinary(Cbor.encodeToByteArray(data))


  private val closeLock = Mutex()

  @OptIn(DelicateCoroutinesApi::class)
  suspend fun close(cause: Throwable? = null, reason: CancellationException? = null) =
    closeLock.withLock {
      if (!income.isClosedForSend) {
        income.cancel(reason)
        income.close(cause)
        outgoing.close(cause)
        outgoing.cancel(reason)
        closeSignal.emit()
      }
    }
}

class PureChannel(
  private val _income: Channel<PureFrame>,
  private val _outgoing: Channel<PureFrame>,
  override var from: Any? = null,
) : IPureChannel, IFrom {
  constructor(from: Any? = null) : this(Channel(), Channel(), from)

  internal val closeSignal = SimpleSignal()
  var isClosed = false
    private set
  override val onClose = closeSignal.toListener().also {
    it.invoke { isClosed = true }
  }

  private val startSignal = Signal<PureChannelContext>()
  override val onStart = startSignal.toListener()


  private val _start = SuspendOnce {
    PureChannelContext(
      income = _income,
      outgoing = _outgoing,
      closeSignal = closeSignal
    ) { this }
      .also { startSignal.emit(it) }
  }

  override suspend fun start() = _start()
  suspend fun afterStart(): PureChannelContext {
    if (!_start.haveRun) {
      onStart.awaitOnce()
    }
    return _start()
  }

  override suspend fun close(cause: Throwable?, reason: CancellationException?) {
    _start().close(cause, reason)
  }

  suspend fun afterClose() {
    if (!isClosed) {
      onClose.awaitOnce()
    }
  }

  private val _remote = atomic<PureChannel?>(null)

  fun reverse() = _remote.updateAndGet {
    it ?: PureChannel(
      _outgoing,
      _income,
      this
    ).also { it._remote.update { this@PureChannel } }
  }!!
}


@Serializable
sealed class PureFrame

@Serializable
@SerialName("text")
class PureTextFrame(val data: String) : PureFrame()

@Serializable
@SerialName("binary")
class PureBinaryFrame(val data: ByteArray) : PureFrame()

@Serializable
@SerialName("close")
data object PureCloseFrame : PureFrame()
//
//@Serializable
//@SerialName("ping")
//data object PurePingFrame : PureFrame()
//
//@Serializable
//@SerialName("pong")
//data object PurePongFrame : PureFrame()


val HttpStatusCode.Companion.WS_CLOSE_NORMAL by lazy { HttpStatusCode(1000, "Close normal") }
val HttpStatusCode.Companion.WS_CLOSE_GOING_AWAY by lazy {
  HttpStatusCode(
    1001,
    "Close going away"
  )
}
val HttpStatusCode.Companion.WS_CLOSE_PROTOCOL_ERROR by lazy {
  HttpStatusCode(
    1002,
    "Close protocol error"
  )
}
val HttpStatusCode.Companion.WS_CLOSE_UNSUPPORTED by lazy {
  HttpStatusCode(
    1003,
    "Close unsupported"
  )
}
val HttpStatusCode.Companion.WS_CLOSED_NO_STATUS by lazy {
  HttpStatusCode(
    1005,
    "Closed no status"
  )
}
val HttpStatusCode.Companion.WS_CLOSE_ABNORMAL by lazy { HttpStatusCode(1006, "Close abnormal") }
val HttpStatusCode.Companion.WS_UNSUPPORTED_PAYLOAD by lazy {
  HttpStatusCode(
    1007,
    "Unsupported payload"
  )
}
val HttpStatusCode.Companion.WS_POLICY_VIOLATION by lazy {
  HttpStatusCode(
    1008,
    "Policy violation"
  )
}
val HttpStatusCode.Companion.WS_CLOSE_TOO_LARGE by lazy { HttpStatusCode(1009, "Close too large") }
val HttpStatusCode.Companion.WS_MANDATORY_EXTENSION by lazy {
  HttpStatusCode(
    1010,
    "Mandatory extension"
  )
}
val HttpStatusCode.Companion.WS_SERVER_ERROR by lazy { HttpStatusCode(1011, "Server error") }
val HttpStatusCode.Companion.WS_SERVICE_RESTART by lazy { HttpStatusCode(1012, "Service restart") }
val HttpStatusCode.Companion.WS_TRY_AGAIN_LATER by lazy { HttpStatusCode(1013, "Try again later") }
val HttpStatusCode.Companion.WS_BAD_GATEWAY by lazy { HttpStatusCode(1014, "Bad gateway") }
val HttpStatusCode.Companion.WS_TLS_HANDSHAKE_FAIL by lazy {
  HttpStatusCode(
    1015,
    "TLS handshake fail"
  )
}
