package info.bagen.rust.plaoc.microService.helper

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.toPath

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


inline fun ByteArray.toInt(): Int {
    return ByteBuffer.wrap(this).int
}

inline fun Int.toByteArray(): ByteArray {
    val bb4 = ByteBuffer.allocate(4)
    return bb4.putInt(0, this).array()
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

inline fun String.asBase64(): ByteArray = base64Decoder.decode(this)

inline fun String.asUtf8(): ByteArray = this.toByteArray(Charsets.UTF_8)

inline fun String.encodeURIComponent(): String = URLEncoder.encode(this, "UTF-8")
    .replace("\\+", "%20")
    .replace("\\%21", "!")
    .replace("\\%27", "'")
    .replace("\\%28", "(")
    .replace("\\%29", ")")
    .replace("\\%7E", "~");

inline fun String.encodeURI(): String = URLEncoder.encode(this, "UTF-8")
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



