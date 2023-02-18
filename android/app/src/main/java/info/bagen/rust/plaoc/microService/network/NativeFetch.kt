package info.bagen.rust.plaoc.microService.network

import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.MicroModule
import info.bagen.rust.plaoc.microService.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.*
import org.http4k.client.OkHttp
import org.http4k.core.*
import org.http4k.core.Method
import kotlin.io.path.Path

var fetchAdaptor: (suspend (remote: MicroModule, request: Request) -> Response?)? = null

/**
 * 加载本地文件
 */
private suspend fun localeFileFetch(remote: MicroModule, request: Request) =
    when {
        request.uri.scheme == "file" && request.uri.path == "" -> runCatching {
            App.appContext.assets.open(request.uri.path).use {
                Response(status = Status.OK).body(it)
            }
        }.getOrElse {
            Response(Status.NOT_FOUND).body("the ${request.uri.path} file not found.")
        }
        else -> null
    }

val client = OkHttp()

suspend fun NativeMicroModule.nativeFetch(request: Request): Response {
    return fetchAdaptor?.let { it(this, request) }
        ?: localeFileFetch(this, request)
        ?: client(request)
}

suspend inline fun NativeMicroModule.nativeFetch(url: Uri) = nativeFetch(Request(Method.GET, url))
suspend inline fun NativeMicroModule.nativeFetch(url: String) = nativeFetch(Request(Method.GET, url))

