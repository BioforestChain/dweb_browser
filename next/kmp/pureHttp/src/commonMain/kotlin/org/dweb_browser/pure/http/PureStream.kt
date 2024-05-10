package org.dweb_browser.pure.http

import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.ioAsyncExceptionHandler

open class PureStream(private val readChannel: ByteReadChannel) {
  constructor(byteArray: ByteArray) : this(ByteReadChannel(byteArray))

  private var opened: String? = null
  val isOpened get() = opened != null
  private val openedDeferred = CompletableDeferred<Unit>()
  val afterOpened: Deferred<Unit> =  openedDeferred
  fun getReader(reason: String): ByteReadChannel {
    if (opened != null) {
      throw Exception("stream already been read: $opened")
    }
    opened = reason
    openedDeferred.complete(Unit)
    return readChannel
  }

  private val _onCloseRead by lazy {
    val signal  = SimpleSignal()
    CoroutineScope(ioAsyncExceptionHandler).launch {
      while (!readChannel.isClosedForRead){
        delay(1000)
      }
      signal.emitAndClear()
    }
    signal
  }
  val onClose by lazy { _onCloseRead.toListener() }

  override fun toString() = "PureStream[$readChannel]"

  fun toBody() = PureStreamBody(this)
}