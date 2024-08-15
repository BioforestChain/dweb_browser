package org.dweb_browser.helper

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.bits.copyTo
import io.ktor.utils.io.read

public actual suspend inline fun ByteReadChannel.consumeEachArrayRange(
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