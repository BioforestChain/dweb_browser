package info.bagen.rust.plaoc.microService.sys.dns

import android.webkit.MimeTypeMap
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.MicroModule
import org.http4k.client.OkHttp
import org.http4k.core.*
import org.http4k.core.Method

var fetchAdaptor: (suspend (remote: MicroModule, request: Request) -> Response?)? = null

val mimeTypeMap by lazy { MimeTypeMap.getSingleton() }
/**
 * 加载本地文件
 */
private suspend fun localeFileFetch(remote: MicroModule, request: Request) =
    when {
        request.uri.scheme == "file" && request.uri.path == "" -> runCatching {
            App.appContext.assets.open(request.uri.path).use {
                Response(status = Status.OK).body(it).also { response ->
                    val extension = MimeTypeMap.getFileExtensionFromUrl(request.uri.path)
                    if (extension != null) {
                        val type = mimeTypeMap.getMimeTypeFromExtension(extension)
                        if (type != null) {
                            response.header("Content-Type", type)
                        }
                    }
                }
            }
        }.getOrElse {
            Response(Status.NOT_FOUND).body("the ${request.uri.path} file not found.")
        }
        else -> null
    }

val client = OkHttp(bodyMode = BodyMode.Stream)

suspend fun MicroModule.nativeFetch(request: Request): Response {
    return fetchAdaptor?.let { it(this, request) }
        ?: localeFileFetch(this, request)
        ?: client(request)
}

suspend inline fun MicroModule.nativeFetch(url: Uri) = nativeFetch(Request(Method.GET, url))
suspend inline fun MicroModule.nativeFetch(url: String) = nativeFetch(Request(Method.GET, url))

