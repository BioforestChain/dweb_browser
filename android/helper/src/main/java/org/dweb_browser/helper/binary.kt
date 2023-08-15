package org.dweb_browser.helper

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

val base64Encoder by lazy {
  Base64.getEncoder()
}
val base64Decoder by lazy {
  Base64.getDecoder()
}
val base64UrlEncoder by lazy {
  Base64.getUrlEncoder()
}

fun ByteArray.byteArrayInputStream(): ByteArrayInputStream = ByteArrayInputStream(this)
fun ByteArray.toBase64(): String = base64Encoder.encodeToString(this)
fun ByteArray.toUtf8(): String = String(this, Charsets.UTF_8)
fun ByteArray.toBase64Url(): String = base64UrlEncoder.encodeToString(this)


///
fun ByteArray.toInt(): Int {
  return ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).int
}

fun Int.toByteArray(): ByteArray {
  val bb4 = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
  return bb4.putInt(0, this).array()
}


private fun InputStream.tryReadByteArray(size: Int): Pair<ByteArray, Int> {
  val bytes = ByteArray(size)
  var offset = 0;
  while (offset < size && available() >= 0) {
    val readLen = read(bytes, offset, size)
    offset += readLen
  }
  return Pair(bytes, offset)
}

fun InputStream.readInt(): Int {
  val (bytes, readLen) = tryReadByteArray(4)
  if (readLen < 4) {
    throw Exception("fail to read int($readLen/4 byte) in stream")
  }
  return bytes.toInt()
}

fun InputStream.readByteArray(size: Int): ByteArray {
  val (bytes, readLen) = tryReadByteArray(size)
  if (readLen < size) {
    throw Exception("fail to read bytes($readLen/$size byte) in stream")
  }
  return bytes
}

operator fun InputStream.iterator(): Iterator<ByteArray> {
  return object : Iterator<ByteArray> {
    override fun hasNext(): Boolean = available() > 0
    override fun next(): ByteArray = readByteArray(available())
  }
}

fun InputStream.readByteArray(): ByteArray {
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

fun String.toBase64ByteArray(): ByteArray = base64Decoder.decode(this)
fun String.toUtf8ByteArray(): ByteArray = this.toByteArray(Charsets.UTF_8)

fun String.encodeURIComponent(): String = URLEncoder.encode(this, "UTF-8")
  .replace("\\+", "%20")
  .replace("\\%21", "!")
  .replace("\\%27", "'")
  .replace("\\%28", "(")
  .replace("\\%29", ")")
  .replace("\\%7E", "~");

fun String.decodeURIComponent(): String = URLDecoder.decode(this, "UTF-8")
  .replace("%20", "\\+")
  .replace("!", "\\%21")
  .replace("'", "\\%27")
  .replace("(", "\\%28")
  .replace(")", "\\%29")
  .replace("~", "\\%7E")

fun String.encodeURI(): String = URLEncoder.encode(this, "UTF-8")
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


fun String.decodeURI(): String = URLDecoder.decode(this, "UTF-8")
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

