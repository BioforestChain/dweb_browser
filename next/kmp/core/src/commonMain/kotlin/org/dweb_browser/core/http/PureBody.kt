package org.dweb_browser.core.http

import io.ktor.util.toByteArray
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.toBase64ByteArray
import org.dweb_browser.helper.toUtf8
import org.dweb_browser.helper.toUtf8ByteArray

typealias PureString = String
typealias PureBase64 = String
typealias PureBinary = ByteArray

sealed interface IPureBody {
  val contentLength: Long?
  fun toPureStream(): PureStream
  suspend fun toPureBinary(): PureBinary
  suspend fun toPureString(): PureString

  companion object {
    val Empty get() = PureEmptyBody()

    enum class PureStringEncoding {
      Utf8,
      Base64,
      ;
    }

    fun from(value: PureString?, encoding: PureStringEncoding = PureStringEncoding.Utf8) =
      when (value) {
        null -> Empty
        else -> if (value.isEmpty()) Empty else when (encoding) {
          PureStringEncoding.Utf8 -> PureStringBody(value)
          PureStringEncoding.Base64 -> PureBinaryBody(value.toBase64ByteArray())
        }
      }

    fun from(value: PureBinary?) = when (value) {
      null -> Empty
      else -> if (value.isEmpty()) Empty else PureBinaryBody(value)
    }

    fun from(value: PureStream?) = when (value) {
      null -> Empty
      else -> PureStreamBody(value)
    }

    private fun fromAny(value: Any?) = when (value) {
      null -> Empty
      is PureString -> if (value.isEmpty()) Empty else PureStringBody(value)
      is PureBinary -> if (value.isEmpty()) Empty else PureBinaryBody(value)
      is PureStream -> PureStreamBody(value)
      else -> Empty
    }
  }
}

class PureBinaryBody(val data: PureBinary) : IPureBody {
  override val contentLength
    get() = data.size.toLong()

  private val stream by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { PureStream(ByteReadChannel(data)) }
  override fun toPureStream() = stream

  override suspend fun toPureBinary() = data

  override suspend fun toPureString() = data.toUtf8()
}

class PureStreamBody(val stream: PureStream) : IPureBody {
  constructor(stream: ByteReadChannel) : this(PureStream(stream))
  constructor(binary: ByteArray) : this(ByteReadChannel(binary)) {
    byteArray = binary
  }

  override var contentLength: Long? = null
    private set

  override fun toPureStream() = stream

  private var byteArray: ByteArray? = null
    set(value) {
      field = value
      contentLength = value?.size?.toLong()
    }
  private val lock = Mutex()
  override suspend fun toPureBinary() = lock.withLock {
    byteArray ?: stream.getReader("PureStreamBody toPureBinary").toByteArray()
      .also {
        byteArray = it;
      }
  }

  override suspend fun toPureString() = toPureBinary().toUtf8()
}


class PureStringBody(val data: PureString) : IPureBody {

  override val contentLength: Long
    get() = data.length.toLong()

  private val stream by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    PureStream(ByteReadChannel(byteArray))
  }

  override fun toPureStream() = stream

  private val byteArray by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { data.toUtf8ByteArray() }
  override suspend fun toPureBinary() = byteArray

  override suspend fun toPureString() = data
}

class PureEmptyBody : IPureBody {
  override val contentLength = 0L

  private val emptyStream by lazy { PureStream(ByteReadChannel(emptyByteArray)) }
  override fun toPureStream() = emptyStream

  override suspend fun toPureBinary() = emptyByteArray

  override suspend fun toPureString() = ""

  companion object {
    private val emptyByteArray = ByteArray(0)
  }
}

