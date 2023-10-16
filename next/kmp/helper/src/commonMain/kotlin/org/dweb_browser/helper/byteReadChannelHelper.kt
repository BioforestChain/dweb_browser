package org.dweb_browser.helper

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.bits.copyTo
import io.ktor.utils.io.read
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readIntLittleEndian
import io.ktor.utils.io.readUTF8Line
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json

val ByteReadChannel.canRead get() = !(availableForRead == 0 && isClosedForWrite && isClosedForRead)
suspend fun ByteReadChannel.canReadContent(): Boolean {
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

suspend fun ByteReadChannel.readAvailablePacket() = readPacket(availableForRead)
suspend fun ByteReadChannel.readAvailableByteArray() =
  ByteArray(availableForRead).also { readAvailable(it) }// readAvailablePacket().readByteArray()

/**
 * For every available bytes range invokes [visitor] function until it return false or end of stream encountered.
 * The provided buffer should be never captured outside of the visitor block otherwise resource leaks, crashes and
 * data corruptions may occur. The visitor block may be invoked multiple times, once or never.
 */
suspend inline fun ByteReadChannel.consumeEachArrayRange(
  visitor: ConsumeEachArrayVisitor,
) {
  val controller = ChannelConsumeEachController()
  do {
    var lastChunkReported = false
    read { source, start, endExclusive ->
      val nioBuffer: ByteArray = when {
        endExclusive > start -> source.slice(start, endExclusive - start).run {
          val remaining = (endExclusive - start).toInt()
          val res = ByteArray(remaining)
          copyTo(res, start, remaining)
          res
        }

        else -> ByteArray(0)
      }

      lastChunkReported = availableForRead == 0 && isClosedForWrite
      controller.visitor(nioBuffer, lastChunkReported)

      nioBuffer.size
    }
    /// 这个要在 isClosedForRead 属性之前，否则会出问题
    if (!controller.continueFlag) {
      break
    }

    if (lastChunkReported && isClosedForRead) {
      break
    }
  } while (true)
}
/**
 * Visitor function that is invoked for every available buffer (or chunk) of a channel.
 * The last parameter shows that the buffer is known to be the last.
 */
typealias ConsumeEachArrayVisitor = ChannelConsumeEachController. (byteArray: ByteArray, last: Boolean) -> Unit

class ChannelConsumeEachController() {
  var continueFlag = true
  fun breakLoop() {
    continueFlag = false
  }
}

suspend inline fun <reified T> ByteReadChannel.consumeEachJsonLine(visitor: ChannelConsumeEachController.(T) -> Unit) {
  val controller = ChannelConsumeEachController()
  while (canRead) {
    val line = readUTF8Line() ?: break
    controller.visitor(Json.decodeFromString<T>(line))
  }
}

@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified T> ByteReadChannel.consumeEachCborPacket(visitor: ChannelConsumeEachController.(T) -> Unit) =
  consumeEachByteArrayPacket {
    this.visitor(Cbor.decodeFromByteArray(it))
  }

suspend inline fun ByteReadChannel.consumeEachByteArrayPacket(visitor: ChannelConsumeEachController.(ByteArray) -> Unit) {
  val controller = ChannelConsumeEachController()
  while (controller.continueFlag) {
    val sizePacket = readPacket(4)
    val sizeBytes = sizePacket.readByteArray()
    val size = sizeBytes.toInt()
    val packet = readPacket(size)
    controller.visitor(packet.readByteArray())
  }
}