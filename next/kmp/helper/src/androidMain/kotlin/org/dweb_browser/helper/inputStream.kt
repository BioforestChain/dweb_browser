package  org.dweb_browser.helper

import java.io.InputStream

private fun InputStream.tryReadByteArray(size: Int): Pair<ByteArray, Int> {
  val bytes = ByteArray(size)
  var offset = 0
  while (offset < size && available() >= 0) {
    val readLen = read(bytes, offset, size)
    offset += readLen
  }
  return Pair(bytes, offset)
}

public fun InputStream.readInt(): Int {
  val (bytes, readLen) = tryReadByteArray(4)
  if (readLen < 4) {
    throw Exception("fail to read int($readLen/4 byte) in stream")
  }
  return bytes.toLittleEndianInt()
}

public fun InputStream.readByteArray(size: Int): ByteArray {
  val (bytes, readLen) = tryReadByteArray(size)
  if (readLen < size) {
    throw Exception("fail to read bytes($readLen/$size byte) in stream")
  }
  return bytes
}

public operator fun InputStream.iterator(): Iterator<ByteArray> {
  return object : Iterator<ByteArray> {
    override fun hasNext(): Boolean = available() > 0
    override fun next(): ByteArray = readByteArray(available())
  }
}

public fun InputStream.readByteArray(): ByteArray {
  var bytes = ByteArray(0)
  while (true) {
    val availableSize = available()
    if (availableSize <= 0) {
      break
    }
    bytes += readByteArray(availableSize)
  }
  return bytes
}