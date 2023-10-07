package org.dweb_browser.helper

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.bits.copyTo
import io.ktor.utils.io.read
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readUTF8Line
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
  val controller = ConsumeEachArrayRangeController()
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

    if (lastChunkReported && isClosedForRead) {
      break
    }
  } while (controller.continueFlag)
}
/**
 * Visitor function that is invoked for every available buffer (or chunk) of a channel.
 * The last parameter shows that the buffer is known to be the last.
 */
typealias ConsumeEachArrayVisitor = ConsumeEachArrayRangeController. (byteArray: ByteArray, last: Boolean) -> Unit

class ConsumeEachArrayRangeController() {
  var continueFlag = true
  fun breakLoop() {
    continueFlag = false
  }
}

suspend inline fun <reified T> ByteReadChannel.consumeEachJsonLine(visitor: T.() -> Unit) {
  while (canRead) {
    val line = readUTF8Line() ?: break
    Json.decodeFromString<T>(line).visitor()
  }
}