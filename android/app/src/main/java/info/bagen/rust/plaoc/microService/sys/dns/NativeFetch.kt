package info.bagen.rust.plaoc.microService.sys.dns

import android.webkit.MimeTypeMap
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.MicroModule
import org.http4k.client.OkHttp
import org.http4k.core.*
import org.http4k.core.Method

typealias FetchAdapter = suspend (remote: MicroModule, request: Request) -> Response?

class NativeFetchAdaptersManager {
    private val adapterOrderMap = mutableMapOf<FetchAdapter, Int>()
    private var orderdAdapters = listOf<FetchAdapter>()
    val adapters get() = orderdAdapters
    fun append(order: Int = 0, adapter: FetchAdapter): () -> Boolean {
        adapterOrderMap[adapter] = order
        orderdAdapters =
            adapterOrderMap.toList().sortedBy { (_, b) -> b }.map { (adapter) -> adapter }
        return { remove(adapter) }
    }

    fun remove(adapter: FetchAdapter) = adapterOrderMap.remove(adapter) != null
}

val nativeFetchAdaptersManager = NativeFetchAdaptersManager()

private val mimeTypeMap by lazy { MimeTypeMap.getSingleton() }

/**
 * 加载本地文件
 */
private fun localeFileFetch(remote: MicroModule, request: Request) =
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

val networkFetch = OkHttp(bodyMode = BodyMode.Stream)

suspend fun MicroModule.nativeFetch(request: Request): Response {
    for (fetchAdapter in nativeFetchAdaptersManager.adapters) {
        val response = fetchAdapter(this, request)
        if (response != null) {
            return response
        }
    }
    return localeFileFetch(this, request) ?: networkFetch(request)
}

suspend inline fun MicroModule.nativeFetch(url: Uri) = nativeFetch(Request(Method.GET, url))
suspend inline fun MicroModule.nativeFetch(url: String) = nativeFetch(Request(Method.GET, url))

