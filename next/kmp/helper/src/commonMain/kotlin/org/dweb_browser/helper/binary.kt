package org.dweb_browser.helper

import io.ktor.http.decodeURLPart
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLPath
import io.ktor.http.encodeURLQueryComponent

fun String.toBase64ByteArray(): ByteArray = Base64.decode(this, false)

/**
 * Converts 4 [Byte]s with [LITTLE_ENDIAN] ordering to a [Int]
 * */
fun bytesToLittleEndianInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int {
  return ((b0.toInt() and 0xff)) or
      ((b1.toInt() and 0xff) shl 8) or
      ((b2.toInt() and 0xff) shl 16) or
      ((b3.toInt()) shl 24)
}

fun ByteArray.toLittleEndianInt(): Int {
  if (this.size == 4) {
    return bytesToLittleEndianInt(this[0], this[1], this[2], this[3])
  }

  throw Exception("bytearray is not 4 byte to ordering to a int")
}

/**
 * Converts a [Int] to [LITTLE_ENDIAN] ordered bytes
 * */
fun Int.toLittleEndianByteArray(): ByteArray {
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


fun String.encodeURIComponent(): String = this.encodeURLQueryComponent()

fun String.decodeURIComponent(): String = this.decodeURLQueryComponent()

fun String.encodeURI(): String = this.encodeURLPath()

fun String.decodeURI(): String = this.decodeURLPart()
