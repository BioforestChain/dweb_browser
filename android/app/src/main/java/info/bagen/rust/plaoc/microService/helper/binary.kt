package info.bagen.rust.plaoc.microService.helper

import info.bagen.rust.plaoc.App
import java.io.InputStream
import java.net.URLEncoder
import java.util.*

inline fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)
inline fun ByteArray.toBase64Url(): String = Base64.getUrlEncoder().encodeToString(this)

inline fun ByteArray.toInt() =
    (this[0].toInt() shl 24) or (this[1].toInt() shl 16) or (this[2].toInt() shl 8) or (this[3].toInt())

inline fun Int.toByteArray(): ByteArray {
    val bytes = ByteArray(4)
    // The resulting `Byte` value is represented by the least significant 8 bits of this `Int` value.
    bytes[3] = this.toByte()
    bytes[2] = (this ushr 8).toByte()
    bytes[1] = (this ushr 16).toByte()
    bytes[0] = (this ushr 24).toByte()
    return bytes
}

inline fun InputStream.readInt(): Int {
    val bytes = ByteArray(4)
    if (read(bytes) != bytes.size) {
        throw Exception("fail to read int(4 byte) in stream")
    }
    return bytes.toInt()
}

inline fun InputStream.readByteArray(size: Int): ByteArray {
    val bytes = ByteArray(size)
    if (read(bytes) != bytes.size) {
        throw Exception("fail to read bytes($size byte) in stream")
    }
    return bytes
}

inline fun String.asBase64(): ByteArray = Base64.getDecoder().decode(this)

inline fun String.asUtf8(): ByteArray = this.toByteArray(Charsets.UTF_8)

inline fun String.toURLQueryComponent(): String = URLEncoder.encode(this, "UTF-8")

