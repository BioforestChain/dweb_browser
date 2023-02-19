package info.bagen.rust.plaoc.microService.helper

import info.bagen.rust.plaoc.App
import java.io.InputStream
import java.net.URLEncoder
import java.util.*

inline fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)
inline fun ByteArray.toBase64Url(): String = Base64.getUrlEncoder().encodeToString(this)

inline fun ByteArray.toLong() =
    (this[0].toLong() shl 24) or (this[1].toLong() shl 16) or (this[2].toLong() shl 8) or (this[3].toLong())

inline fun Long.toByteArray(): ByteArray {
    val bytes = ByteArray(4)
    bytes[3] = (this and 0xFFFF).toByte()
    bytes[2] = ((this ushr 8) and 0xFFFF).toByte()
    bytes[1] = ((this ushr 16) and 0xFFFF).toByte()
    bytes[0] = ((this ushr 24) and 0xFFFF).toByte()
    return bytes
}

inline fun InputStream.readLong() = this.readBytes().toLong()

inline fun String.asBase64(): ByteArray = Base64.getDecoder().decode(this)

inline fun String.asUtf8(): ByteArray = this.toByteArray(Charsets.UTF_8)

inline fun String.toURLQueryComponent(): String = URLEncoder.encode(this, "UTF-8")

