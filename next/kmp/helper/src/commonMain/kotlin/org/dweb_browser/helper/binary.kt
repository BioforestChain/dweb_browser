package org.dweb_browser.helper

import io.ktor.http.URLBuilder
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readAvailable

fun ByteArray.byteArrayInputStream(): ByteReadPacket = ByteReadPacket(this)
fun ByteArray.toUtf8(): String = io.ktor.utils.io.core.String(this, 0, size, Charsets.UTF_8)

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

  return bytes.toInt()
}

fun ByteReadPacket.readByteArray(size: Int): ByteArray {
  val (bytes, readLen) = tryReadByteArray(size)
  if (readLen < size) {
    throw Exception("fail to read bytes($readLen/$size byte) in stream")
  }

  return bytes
}

fun String.toBase64ByteArray(): ByteArray = Base64.decode(this, false)
fun String.toUtf8ByteArray(): ByteArray = this.encodeToByteArray()

/**
 * Converts 4 [Byte]s with [LITTLE_ENDIAN] ordering to a [Int]
 * */
fun bytesToInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int {
  return ((b0.toInt() and 0xff)) or
      ((b1.toInt() and 0xff) shl 8) or
      ((b2.toInt() and 0xff) shl 16) or
      ((b3.toInt()) shl 24)
}

fun ByteArray.toInt(): Int {
  if (this.size == 4) {
    return bytesToInt(this[0], this[1], this[2], this[3])
  }

  throw Exception("bytearray is not 4 byte to ordering to a int")
}

/**
 * Converts a [Int] to [LITTLE_ENDIAN] ordered bytes
 * */
fun Int.toByteArray(): ByteArray {
  val b0 = this.toByte()
  val b1 = (this ushr 8).toByte()
  val b2 = (this ushr 16).toByte()
  val b3 = (this ushr 24).toByte()
  return ByteArray(4).apply {
    this[0] = b0
    this[1] = b1
    this[2] = b2
    this[3] = b3
  }
}

operator fun ByteReadPacket.iterator(): Iterator<ByteArray> {
  return object : Iterator<ByteArray> {
    override fun hasNext(): Boolean = remaining > 0
    override fun next(): ByteArray = readByteArray(remaining.toInt())
  }
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

fun String.encodeURIComponent(): String = URLBuilder(this).encodedFragment
  .replace("\\+", "%20")
  .replace("\\%21", "!")
  .replace("\\%27", "'")
  .replace("\\%28", "(")
  .replace("\\%29", ")")
  .replace("\\%7E", "~")

fun String.decodeURIComponent(): String = URLBuilder(this).fragment
  .replace("%20", "\\+")
  .replace("!", "\\%21")
  .replace("'", "\\%27")
  .replace("(", "\\%28")
  .replace(")", "\\%29")
  .replace("~", "\\%7E")

fun String.encodeURI(): String = URLBuilder(this).encodedFragment
  .replace("%3B", ";")
  .replace("%2F", "/")
  .replace("%3F", "?")
  .replace("%3A", ":")
  .replace("%40", "@")
  .replace("%26", "&")
  .replace("%3D", "=")
  .replace("%2B", "+")
  .replace("%24", "$")
  .replace("%2C", ",")
  .replace("%23", "#")

fun String.decodeURI(): String = URLBuilder(this).fragment
  .replace(";", "%3B")
  .replace("/", "%2F")
  .replace("?", "%3F")
  .replace(":", "%3A")
  .replace("@", "%40")
  .replace("&", "%26")
  .replace("=", "%3D")
  .replace("+", "%2B")
  .replace("$", "%24")
  .replace(",", "%2C")
  .replace("#", "%23")
