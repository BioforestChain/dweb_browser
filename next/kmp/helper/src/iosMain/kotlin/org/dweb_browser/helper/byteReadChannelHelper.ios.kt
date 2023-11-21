package org.dweb_browser.helper

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.EOFException
import io.ktor.utils.io.core.readBytes

actual suspend inline fun ByteReadChannel.consumeEachArrayRange(
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