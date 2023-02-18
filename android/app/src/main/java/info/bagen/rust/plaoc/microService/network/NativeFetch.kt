package info.bagen.rust.plaoc.microService.network

import info.bagen.rust.plaoc.microService.MicroModule
import info.bagen.rust.plaoc.microService.NativeMicroModule
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Uri

var fetchAdaptor: (suspend (remote: MicroModule, request: Request) -> Response?)? = null

suspend fun localeFileFetch(remote: MicroModule, request: Request): Response? {
    if (request.uri.scheme == "file:" && request.uri.host == "") {
        /// TODO 从本地文件读取
    }
    return null
}

val client = OkHttp()

suspend fun NativeMicroModule.nativeFetch(request: Request): Response {
    return fetchAdaptor?.let { it(this, request) }
        ?: localeFileFetch(this, request)
        ?: client(request)
}

suspend fun NativeMicroModule.nativeFetch(url: Uri) = nativeFetch(Request(Method.GET, url))
suspend fun NativeMicroModule.nativeFetch(url: String) = nativeFetch(Request(Method.GET, url))

