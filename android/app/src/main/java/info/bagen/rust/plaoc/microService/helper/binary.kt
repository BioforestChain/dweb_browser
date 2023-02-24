package info.bagen.rust.plaoc.microService.helper

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.*

val base64Encoder by lazy {
    Base64.getEncoder()
}
val base64Decoder by lazy {
    Base64.getDecoder()
}
val base64UrlEncoder by lazy {
    Base64.getUrlEncoder()
}

inline fun ByteArray.byteArrayInputStream(): ByteArrayInputStream = ByteArrayInputStream(this)
inline fun ByteArray.toBase64(): String = base64Encoder.encodeToString(this)
inline fun ByteArray.toUtf8(): String = String(this, Charsets.UTF_8)
inline fun ByteArray.toBase64Url(): String = base64UrlEncoder.encodeToString(this)

val bb4 = ByteBuffer.allocate(4)

inline fun ByteArray.toInt(): Int {
    bb4.clear()
    return bb4.put(this, 0, 4).getInt(0)
}

inline fun Int.toByteArray() = bb4.putInt(0, this).array()


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

inline fun String.asBase64(): ByteArray = base64Decoder.decode(this)

inline fun String.asUtf8(): ByteArray = this.toByteArray(Charsets.UTF_8)

inline fun String.toURLQueryComponent(): String = URLEncoder.encode(this, "UTF-8")

