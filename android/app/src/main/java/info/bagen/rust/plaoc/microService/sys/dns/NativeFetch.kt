package info.bagen.rust.plaoc.microService.sys.dns

import android.webkit.MimeTypeMap
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.printdebugln
import org.http4k.client.ApacheClient
import org.http4k.core.*

typealias FetchAdapter = suspend (remote: MicroModule, request: Request) -> Response?


inline fun debugFetch(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("fetch", tag, msg, err)

class NativeFetchAdaptersManager {
    private val adapterOrderMap = mutableMapOf<FetchAdapter, Int>()
    private var orderdAdapters = listOf<FetchAdapter>()
    val adapters get() = orderdAdapters
    fun append(order: Int = 0, adapter: FetchAdapter): (Unit) -> Boolean {
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
        request.uri.scheme == "file" && request.uri.host == "" -> runCatching {
            /**
             * 直接整个读出来，避免 AssetInputStream is closed 的错误
             */
            val bodyContent = App.appContext.assets.open(
                /** 移除开头的斜杠 */
                request.uri.path.substring(1)
            ).readBytes()
            Response(status = Status.OK).body(MemoryBody(bodyContent))
                .header("Content-Length", bodyContent.size.toString())
                .let { response ->
                    val extension = MimeTypeMap.getFileExtensionFromUrl(request.uri.path)
                    if (extension != null) {
                        val type = mimeTypeMap.getMimeTypeFromExtension(extension)
                        if (type != null) {
                            return@let response.header("Content-Type", type)
                        }
                    }
                    response
                }
        }.getOrElse {
            Response(Status.NOT_FOUND).body("the ${request.uri.path} file not found.")
        }
        else -> null
    }

val networkFetch =
    ApacheClient(responseBodyMode = BodyMode.Stream, requestBodyMode = BodyMode.Stream)

suspend fun MicroModule.nativeFetch(request: Request): Response {
    for (fetchAdapter in nativeFetchAdaptersManager.adapters) {
        val response = fetchAdapter(this, request)
        if (response != null) {
            return response
        }
    }
    println("request.uri: ${request.uri}")
    return localeFileFetch(this, request) ?: networkFetch(request)
}

suspend inline fun MicroModule.nativeFetch(url: Uri) = nativeFetch(Request(Method.GET, url))
suspend inline fun MicroModule.nativeFetch(url: String) = nativeFetch(Request(Method.GET, url))

