package org.dweb_browser.microservice.http

import io.ktor.util.toByteArray
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.toUtf8
import org.dweb_browser.helper.toUtf8ByteArray

interface IPureBody {
  val contentLength: Long?
  fun toStream(): PureStream
  suspend fun toByteArray(): ByteArray
  suspend fun toUtf8String(): String

  companion object {
    val Empty: PureEmptyBody = PureEmptyBody()
  }
}

open class PureStream(private val readChannel: ByteReadChannel) {
  private var opened = false
  val isOpened get() = opened
  private val openedDeferred = CompletableDeferred<Unit>()
  val afterOpened: Deferred<Unit> = openedDeferred
  fun getReader(): ByteReadChannel {
    if (opened) {
      throw Exception("stream already been read")
    }
    opened = true
    openedDeferred.complete(Unit)
    return readChannel
  }

  override fun toString() = "PureStream[$readChannel]"
}

open class PureByteArrayBody(private val data: ByteArray) : IPureBody {
  override val contentLength: Long?
    get() = data.size.toLong()

  private val stream by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { PureStream(ByteReadChannel(data)) }
  override fun toStream() = stream

  override suspend fun toByteArray(): ByteArray = data

  override suspend fun toUtf8String(): String = data.toUtf8()
}

class PureStreamBody(private val stream: PureStream) : IPureBody {
  constructor(stream: ByteReadChannel) : this(PureStream(stream))

  override val contentLength = null

  override fun toStream() = stream

  private var byteArray: ByteArray? = null
  private val lock = Mutex()
  override suspend fun toByteArray(): ByteArray = lock.withLock {
    byteArray ?: stream.getReader().toByteArray().also { byteArray = it }
  }

  override suspend fun toUtf8String(): String = toByteArray().toUtf8()
}


//class PureBase64StringBody(val xdata: String) : PureByteArrayBody(xdata.toBase64ByteArray())

class PureUtf8StringBody(private val data: String) : IPureBody {

  override val contentLength: Long
    get() = data.length.toLong()

  private val stream by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    PureStream(ByteReadChannel(byteArray))
  }

  override fun toStream() = stream

  private val byteArray by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { data.toUtf8ByteArray() }
  override suspend fun toByteArray(): ByteArray = byteArray

  override suspend fun toUtf8String(): String = data
}

class PureEmptyBody(override val contentLength: Long? = 0L) : IPureBody {
  override fun toStream() = emptyStream

  override suspend fun toByteArray() = emptyByteArray

  override suspend fun toUtf8String() = ""

  companion object {
    val emptyByteArray = ByteArray(0)
    val emptyStream = PureStream(ByteReadChannel(emptyByteArray))
  }
}