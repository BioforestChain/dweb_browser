package info.bagen.rust.plaoc.microService.sys.dns

import android.content.res.AssetManager
import android.webkit.MimeTypeMap
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.AdapterManager
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.helper.readByteArray
import info.bagen.rust.plaoc.microService.ipc.PreReadableInputStream
import org.http4k.client.ApacheClient
import org.http4k.core.*
import java.io.InputStream

typealias FetchAdapter = suspend (remote: MicroModule, request: Request) -> Response?


inline fun debugFetch(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("fetch", tag, msg, err)


val nativeFetchAdaptersManager = AdapterManager<FetchAdapter>()

private val mimeTypeMap by lazy { MimeTypeMap.getSingleton() }


class ChunkAssetsFileStream(
    val source: InputStream,
    val chunkSize: Int = defaultChunkSize,
    override val preReadableSize: Int = chunkSize
) :
    InputStream(), PreReadableInputStream {
    companion object {
        /// 默认1mb切一次
        val defaultChunkSize = 1024 * 1024
    }

    var ptr = 0
    val totalSize = source.available()

    override inline fun read() = source.read()
    override inline fun available() = chunkSize.coerceAtMost(totalSize - ptr)
    override inline fun read(b: ByteArray?, off: Int, len: Int) =
        source.read(b, off, len).also { len ->
            ptr += len
        }

}

/**
 * 加载本地文件
 */
private fun localeFileFetch(remote: MicroModule, request: Request) = when {
    request.uri.scheme == "file" && request.uri.host == "" -> runCatching {
        val mode = request.query("mode") ?: "auto"
        val chunk = request.query("chunk")?.toIntOrNull() ?: ChunkAssetsFileStream.defaultChunkSize
        val preRead = request.query("pre-read")?.toBooleanStrictOrNull() ?: false

        /**
         * 打开一个读取流
         */
        val assetStream = App.appContext.assets.open(
            /** 移除开头的斜杠 */
            request.uri.path.substring(1), when (mode) {
                "buffer" -> AssetManager.ACCESS_BUFFER
                else -> AssetManager.ACCESS_STREAMING
            }
        )

        val totalSize = assetStream.available()
        var response = Response(status = Status.OK).header("Content-Length", totalSize.toString())


        response = if (
        // buffer 模式，就是直接全部读取出来
            mode == "buffer"
            // auto 模式就是在通讯次数和单次通讯延迟之间的一个取舍。如果分片次数少于2次，那么就直接发送，没必要分片
            || (mode == "auto" && totalSize <= chunk * 2)
        ) {
            /**
             * 一次性发送
             */
            response.body(MemoryBody(assetStream.readByteArray(totalSize)))
        } else {
            /**
             * 将它分片读取
             */
            response.body(
                ChunkAssetsFileStream(
                    assetStream,
                    chunkSize = chunk,
                    preReadableSize = if (preRead) chunk else 0
                )
            )
        }

        /// 尝试加入 Content-Type
        val extension = MimeTypeMap.getFileExtensionFromUrl(request.uri.path)
        if (extension != null) {
            val type = mimeTypeMap.getMimeTypeFromExtension(extension)
            if (type != null) {
                response = response.header("Content-Type", type)
            }
        }

        response
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
    return localeFileFetch(this, request) ?: networkFetch(request)
}

suspend inline fun MicroModule.nativeFetch(url: Uri) = nativeFetch(Request(Method.GET, url))
suspend inline fun MicroModule.nativeFetch(url: String) = nativeFetch(Request(Method.GET, url))
