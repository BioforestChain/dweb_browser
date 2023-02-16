package info.bagen.rust.plaoc.microService.ipc.helper

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*

enum class SimpleEncoding(val type:String){
    utf8("utf8"),
    base64("base64")
}

fun simpleEncoder(data: String,encoding: SimpleEncoding): ByteArray {
    val bytes = data.toByteArray(Charsets.UTF_8)
    if (encoding == SimpleEncoding.base64) {
        val encoder: Base64.Encoder = Base64.getEncoder()
        return encoder.encode(bytes)
    }
   return bytes
}

fun simpleDecoder(data: ByteArray,encoding: SimpleEncoding): String{
    if (encoding == SimpleEncoding.base64) {
        val decoder: Base64.Decoder = Base64.getDecoder()
        return decoder.decode(data).toString()
    }
    return  String(data)
}

fun utf8_to_b64 (str: String):String{
    return URLEncoder.encode(str, "utf-8")
};

fun b64_to_utf8(str: String): String {
    return URLDecoder.decode(str, "utf-8")
};

fun dataUrlFromUtf8 (
utf8_string: String,
asBase64: Boolean,
mime: String = ""
): String {
    return if (asBase64) {
        "data:${mime};base64,${b64_to_utf8(utf8_string)}"
    } else {
        "data:${mime};charset=UTF-8,${utf8_to_b64(utf8_string)}";
    }
};
