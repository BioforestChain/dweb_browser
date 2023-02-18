package info.bagen.rust.plaoc.microService.network

import android.webkit.MimeTypeMap
import info.bagen.rust.plaoc.microService.MicroModule
import info.bagen.rust.plaoc.microService.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.openInputStream
import info.bagen.rust.plaoc.microService.helper.readLong
import org.http4k.client.OkHttp
import org.http4k.core.*
import java.io.File
import kotlin.io.path.Path

var fetchAdaptor: (suspend (remote: MicroModule, request: Request) -> Response?)? = null

suspend fun localeFileFetch(remote: MicroModule, request: Request): Response? {
    val prefixUrl = ""
    try {
        val filePath = Path(prefixUrl, request.uri.path).toString()
        println("NativeFetch#localeFileFetch====>$filePath")
        val stats = filePath.openInputStream()
            ?: return  Response(Status.NOT_FOUND).body("the ${request.uri.path} file not found ")

//        val ext = stats.extension
//        val mime: MimeTypeMap = MimeTypeMap.getSingleton()
//        val type: String = mime.getExtensionFromMimeType(ext) ?: "application/octet-stream"
        return  Response(status = Status.OK).body(stats)
//            .headers(
//                headers = listOf(
//                    Pair("Content-Length", stats.readLong().toString()),
//                    Pair("Content-Type", type)
//                )
//            )
    }catch (e: Throwable) {
        return  Response(Status.NOT_FOUND).body(e.message?:"the ${request.uri.path} file not found ")
    }
}

val client = OkHttp()

suspend fun NativeMicroModule.nativeFetch(request: Request): Response {
    return fetchAdaptor?.let { it(this, request) }
        ?: localeFileFetch(this, request)
        ?: client(request)
}

suspend fun NativeMicroModule.nativeFetch(url: Uri) = nativeFetch(Request(Method.GET, url))
suspend fun NativeMicroModule.nativeFetch(url: String) = nativeFetch(Request(Method.GET, url))

