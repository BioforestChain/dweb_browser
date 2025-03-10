package org.dweb_browser.helper

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.bits.reverseByteOrder
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.EOFException
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json

public val ByteReadChannel.canRead: Boolean get() = !(availableForRead == 0 && isClosedForWrite && isClosedForRead)
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

public suspend fun ByteReadChannel.readAvailablePacket(): ByteReadPacket =
  readPacket(availableForRead)

public suspend fun ByteReadChannel.readAvailableByteArray(): ByteArray =
  ByteArray(availableForRead).also { readAvailable(it) }// readAvailablePacket().readByteArray()

/**
 * For every available bytes range invokes [visitor] function until it return false or end of stream encountered.
 * The provided buffer should be never captured outside of the visitor block otherwise resource leaks, crashes and
 * data corruptions may occur. The visitor block may be invoked multiple times, once or never.
 */
public expect suspend inline fun ByteReadChannel.consumeEachArrayRange(visitor: ConsumeEachArrayVisitor)

public suspend inline fun ByteReadChannel.commonConsumeEachArrayRange(
  visitor: ConsumeEachArrayVisitor,
) {
  val controller = ChannelConsumeEachController()
  try {
    do {
      awaitContent()
      val lastChunkReported = availableForRead == 0 && isClosedForWrite
      if (lastChunkReported) {
        controller.visitor(byteArrayOf(), true)
      } else {
        val bytes = readPacket(availableForRead).readBytes()
        controller.visitor(bytes, false)
      }
      /// 这个要在 isClosedForRead 属性之前，否则会出问题
      if (!controller.continueFlag) {
        break
      }

      if (lastChunkReported && isClosedForRead) {
        break
      }
    } while (true)
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
      val sizePacket = readPacket(4)
      val sizeBytes = sizePacket.readByteArray()
      val size = sizeBytes.toLittleEndianInt()
      val packet = readPacket(size)
      controller.visitor(packet.readByteArray())
    }
  } catch (e: ClosedReceiveChannelException) {
    // closed
  } catch (e: EOFException) {
    // closed
  }
}

public suspend fun ByteReadChannel.readIntLittleEndian(): Int = readInt().reverseByteOrder()