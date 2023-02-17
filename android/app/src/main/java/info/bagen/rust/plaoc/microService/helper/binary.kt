package info.bagen.rust.plaoc.microService.helper

import java.io.InputStream
import java.util.*

fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)

fun ByteArray.toLong() =
    (this[0].toLong() shl 24) or (this[1].toLong() shl 16) or (this[2].toLong() shl 8) or (this[3].toLong())

fun Long.toByteArray(): ByteArray {
    val bytes = ByteArray(4)
    bytes[3] = (this and 0xFFFF).toByte()
    bytes[2] = ((this ushr 8) and 0xFFFF).toByte()
    bytes[1] = ((this ushr 16) and 0xFFFF).toByte()
    bytes[0] = ((this ushr 24) and 0xFFFF).toByte()
    return bytes
}

fun InputStream.readLong(): Long {
    return this.readBytes().toLong()
}


fun String.asBase64(): ByteArray = Base64.getDecoder().decode(this)