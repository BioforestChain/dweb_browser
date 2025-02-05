package org.dweb_browser.helper

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.bits.reverseByteOrder
import io.ktor.utils.io.read
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readInt
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.io.EOFException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json

public val ByteReadChannel.canRead: Boolean get() = !(availableForRead == 0 && isClosedForRead)
public suspend fun ByteReadChannel.canReadContent(): Boolean {
  do {
    if (availableForRead > 0) {
      return true
    }
    if (isClosedForRead) {
      return false
    }
    awaitContent()
  } while (true)
}


public suspend fun ByteReadChannel.readAvailableByteArray(): ByteArray =
  readByteArray(availableForRead)

/**
 * For every available bytes range invokes [visitor] function until it return false or end of stream encountered.
 * The provided buffer should be never captured outside of the visitor block otherwise resource leaks, crashes and
 * data corruptions may occur. The visitor block may be invoked multiple times, once or never.
 */
public suspend inline fun ByteReadChannel.consumeEachArrayRange(
  visitor: ConsumeEachArrayVisitor,
) {
  val controller = ChannelConsumeEachController()
//  this.read { bytes, i, i2 ->  }

  try {
    do {
      var byteArray: ByteArray? = null
      read { data, startOffset, endExclusive ->
        byteArray = data.sliceArray(startOffset..<endExclusive)
        endExclusive - startOffset
      }
      when (val bs = byteArray) {
        null -> break
        else -> controller.visitor(bs, false)
      }
    } while (controller.continueFlag)
    controller.visitor(byteArrayOf(), true)
  } catch (e: EOFException) {
    e.printStackTrace()
  }
}

/**
 * Visitor function that is invoked for every available buffer (or chunk) of a channel.
 * The last parameter shows that the buffer is known to be the last.
 */
public typealias ConsumeEachArrayVisitor = ChannelConsumeEachController. (byteArray: ByteArray, last: Boolean) -> Unit

public class ChannelConsumeEachController() {
  public var continueFlag: Boolean = true
  public fun breakLoop() {
    continueFlag = false
  }
}

public suspend inline fun <reified T> ByteReadChannel.consumeEachJsonLine(visitor: ChannelConsumeEachController.(T) -> Unit) {
  val controller = ChannelConsumeEachController()
  while (canRead) {
    val line = readUTF8Line() ?: break
    controller.visitor(Json.decodeFromString<T>(line))
  }
}

@OptIn(ExperimentalSerializationApi::class)
public suspend inline fun <reified T> ByteReadChannel.consumeEachCborPacket(visitor: ChannelConsumeEachController.(T) -> Unit): Unit =
  consumeEachByteArrayPacket {
    this.visitor(Cbor.decodeFromByteArray(it))
  }

public suspend inline fun ByteReadChannel.consumeEachByteArrayPacket(visitor: ChannelConsumeEachController.(ByteArray) -> Unit) {
  val controller = ChannelConsumeEachController()
  try {
    while (controller.continueFlag) {
      val sizeBytes = readByteArray(4)
      val size = sizeBytes.toLittleEndianInt()
      val packetBytes = readByteArray(size)
      controller.visitor(packetBytes)
    }
  } catch (e: ClosedReceiveChannelException) {
    // closed
  } catch (e: EOFException) {
    // closed
  }
}

public suspend fun ByteReadChannel.readIntLittleEndian(): Int = readInt().reverseByteOrder()
