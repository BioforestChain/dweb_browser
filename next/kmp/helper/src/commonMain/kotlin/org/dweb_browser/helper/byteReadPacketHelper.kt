package org.dweb_browser.helper

import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readAvailable

private fun ByteReadPacket.tryReadByteArray(size: Int): Pair<ByteArray, Int> {
  var bytes = ByteArray(size)
  var offset = 0
  while (offset < size && remaining > 0) {
    var readLen = readAvailable(bytes, offset, size)
    offset += readLen
  }

  return Pair(bytes, offset)
}

fun ByteReadPacket.readInt(): Int {
  var (bytes, readLen) = tryReadByteArray(4)
  if (readLen < 4) {
    throw Exception("fail to read int($readLen/4 byte) in stream")
  }

  return bytes.toLittleEndianInt()
}

fun ByteReadPacket.readByteArray(): ByteArray {
  var bytes = ByteArray(0)
  while (true) {
    val availableSize = remaining
    if (availableSize <= 0) {
      break
    }
    bytes += readByteArray(availableSize.toInt())
  }
  return bytes
}

fun ByteReadPacket.readByteArray(size: Int): ByteArray {
  val (bytes, readLen) = tryReadByteArray(size)
  if (readLen < size) {
    throw Exception("fail to read bytes($readLen/$size byte) in stream")
  }

  return bytes
}