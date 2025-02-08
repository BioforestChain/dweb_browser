package org.dweb_browser.pure.http

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.InternalAPI
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.DeferredSignal
import org.dweb_browser.helper.IFrom
import org.dweb_browser.helper.Once
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.helper.utf8String


interface IPureChannel {
  fun start(): PureChannelContext
  val onClose: DeferredSignal<Throwable?>
  fun close(cause: Throwable? = null)
}

class PureChannelContext internal constructor(
  @InternalAPI val incomeChannel: Channel<PureFrame>,
  @InternalAPI val outgoingChannel: Channel<PureFrame>,
  val getChannel: () -> PureChannel,
) {
  @OptIn(InternalAPI::class)
  val income = incomeChannel as ReceiveChannel<PureFrame>
  @OptIn(InternalAPI::class)
  val outgoing = outgoingChannel as SendChannel<PureFrame>
  operator fun iterator() = income.iterator()
  inline fun <reified T> readJsonItems() = flow {
    for (frame in income) {
      emit(Json.decodeFromString<T>(frame.text))
    }
  }

  @OptIn(InternalAPI::class)
  suspend fun sendText(data: String) = runCatching { outgoingChannel.send(PureTextFrame(data)) }

  suspend inline fun <reified T> sendJson(data: T) = sendText(Json.encodeToString(data))

  suspend inline fun <reified T> sendJsonLine(data: T) = sendText(Json.encodeToString(data) + "\n")


  @OptIn(InternalAPI::class)
  suspend fun sendBinary(data: ByteArray) = runCatching { outgoingChannel.send(PureBinaryFrame(data)) }


  @OptIn(ExperimentalSerializationApi::class)
  suspend inline fun <reified T> sendCbor(data: T) = sendBinary(Cbor.encodeToByteArray(data))


  fun close(cause: Throwable? = null) = getChannel().close(cause)
  val isClosed get() = getChannel().isClosed

  @OptIn(InternalAPI::class)
  val onClose = outgoingChannel::invokeOnClose
}

class PureChannel(
  private val _income: Channel<PureFrame>,
  private val _outgoing: Channel<PureFrame>,
  override var from: Any? = null,
) : IPureChannel, IFrom {
  constructor(from: Any? = null) : this(
    _income = Channel(),
    _outgoing = Channel(),
    from = from,
  )

  var isClosed = false
    private set


  private val _start = Once {
    PureChannelContext(
      incomeChannel = _income,
      outgoingChannel = _outgoing,
      getChannel = { this@PureChannel },
    )
  }

  override fun start() = _start()


  private val closeDeferred = CompletableDeferred<Throwable?>()
  override val onClose = DeferredSignal(closeDeferred)

  private val closeLock = SynchronizedObject()

  @OptIn(DelicateCoroutinesApi::class)
  override fun close(cause: Throwable?) {
    synchronized(closeLock) {
      closeDeferred.complete(cause)
      if (!_income.isClosedForSend) {
        _income.close(cause)
      }
      if (!_outgoing.isClosedForSend) {
        _outgoing.close(cause)
      }
    }
  }

  /**
   * 只关闭输出
   */
  @OptIn(DelicateCoroutinesApi::class)
  fun closeOutgoing(cause: Throwable? = null) {
    synchronized(closeLock) {
      closeDeferred.complete(cause)
      if (!_outgoing.isClosedForSend) {
        _outgoing.close(cause)
      }
    }
  }

  private var remote: PureChannel? = null

  companion object {
    private val remoteLock = SynchronizedObject()
  }

  fun reverse() = synchronized(remoteLock) {
    remote ?: PureChannel(
      _outgoing, _income, this
    ).also { remote ->
      this.remote = remote
      remote.remote = this
    }
  }
}


@Serializable
sealed class PureFrame {
  abstract val text: String
  abstract val binary: ByteArray
}

@Serializable
@SerialName("text")
class PureTextFrame(override val text: String) : PureFrame() {
  override val binary get() = text.utf8Binary
  override fun toString(): String {
    return "PureTextFrame(${
      when (val len = text.length) {
        in 0..100 -> text
        else -> text.slice(0..19) + "..." + text.slice(len - 20..<len)
      }
    })"
  }
}

@Serializable
@SerialName("binary")
class PureBinaryFrame(override val binary: ByteArray) : PureFrame() {
  override val text get() = binary.utf8String
  override fun toString(): String {
    return "PureBinaryFrame(size=${binary.size})"
  }
}


val HttpStatusCode.Companion.WS_CLOSE_NORMAL by lazy { HttpStatusCode(1000, "Close normal") }
val HttpStatusCode.Companion.WS_CLOSE_GOING_AWAY by lazy {
  HttpStatusCode(
    1001, "Close going away"
  )
}
val HttpStatusCode.Companion.WS_CLOSE_PROTOCOL_ERROR by lazy {
  HttpStatusCode(
    1002, "Close protocol error"
  )
}
val HttpStatusCode.Companion.WS_CLOSE_UNSUPPORTED by lazy {
  HttpStatusCode(
    1003, "Close unsupported"
  )
}
val HttpStatusCode.Companion.WS_CLOSED_NO_STATUS by lazy {
  HttpStatusCode(
    1005, "Closed no status"
  )
}
val HttpStatusCode.Companion.WS_CLOSE_ABNORMAL by lazy { HttpStatusCode(1006, "Close abnormal") }
val HttpStatusCode.Companion.WS_UNSUPPORTED_PAYLOAD by lazy {
  HttpStatusCode(
    1007, "Unsupported payload"
  )
}
val HttpStatusCode.Companion.WS_POLICY_VIOLATION by lazy {
  HttpStatusCode(
    1008, "Policy violation"
  )
}
val HttpStatusCode.Companion.WS_CLOSE_TOO_LARGE by lazy { HttpStatusCode(1009, "Close too large") }
val HttpStatusCode.Companion.WS_MANDATORY_EXTENSION by lazy {
  HttpStatusCode(
    1010, "Mandatory extension"
  )
}
val HttpStatusCode.Companion.WS_SERVER_ERROR by lazy { HttpStatusCode(1011, "Server error") }
val HttpStatusCode.Companion.WS_SERVICE_RESTART by lazy { HttpStatusCode(1012, "Service restart") }
val HttpStatusCode.Companion.WS_TRY_AGAIN_LATER by lazy { HttpStatusCode(1013, "Try again later") }
val HttpStatusCode.Companion.WS_BAD_GATEWAY by lazy { HttpStatusCode(1014, "Bad gateway") }
val HttpStatusCode.Companion.WS_TLS_HANDSHAKE_FAIL by lazy {
  HttpStatusCode(
    1015, "TLS handshake fail"
  )
}
