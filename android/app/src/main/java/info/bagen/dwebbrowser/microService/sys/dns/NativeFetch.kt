package info.bagen.dwebbrowser.microService.sys.dns

import android.content.res.AssetManager
import android.webkit.MimeTypeMap
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.core.MicroModule
import info.bagen.dwebbrowser.microService.helper.AdapterManager
import info.bagen.dwebbrowser.microService.helper.printdebugln
import info.bagen.dwebbrowser.microService.helper.readByteArray
import info.bagen.dwebbrowser.microService.ipc.PreReadableInputStream
import org.http4k.client.ApacheClient
import org.http4k.core.*
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

typealias FetchAdapter = suspend (remote: MicroModule, request: Request) -> Response?


inline fun debugFetch(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("fetch", tag, msg, err)

inline fun debugFetchFile(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("fetch-file", tag, msg, err)


val nativeFetchAdaptersManager = AdapterManager<FetchAdapter>()

private val mimeTypeMap by lazy { MimeTypeMap.getSingleton() }


/**
 * 安全的 AssetManager 文件读取工具
 */
class ChunkAssetsFileStream(
    val src: String,
    val chunkSize: Int = defaultChunkSize,
    override val preReadableSize: Int = chunkSize
) : InputStream(), PreReadableInputStream {
    companion object {
        /// 默认1mb切一次
        val defaultChunkSize = 1024 * 1024
        private var acc_id = AtomicInteger(1)
    }

    val id = acc_id.getAndAdd(1)

    init {
        debugFetchFile("INIT", "$id/$src")
    }

    val source by lazy {
        App.appContext.assets.open(
            src, AssetManager.ACCESS_STREAMING
        )
    }

    var ptr = 0
    val totalSize by lazy { source.available() }

    override inline fun read() = source.read()
    override inline fun available() = chunkSize.coerceAtMost(totalSize - ptr)

    var readEnd = false

    @Synchronized
    override fun read(b: ByteArray, off: Int, len: Int) = when (readEnd) {
        false -> source.read(b, off, len).also { readLen ->
            if (readLen != len) {
                // assets 的流内容是明确的，所以一般不会有 available 突变的问题，除非被重写
                readEnd = true
                debugFetchFile("READ", "$id/$src($totalSize) $off+$len~>$readLen")
            }
            if (readLen == -1) {
                readEnd = true
            }
//            else if (src.endsWith(".js")) {
//                debugFetchFile(
//                    "READ-CONTENT", "$id/$src: ${
//                        b.slice(0 until readLen).toByteArray().toUtf8().replace(
//                            Regex("\n", RegexOption.MULTILINE), " ↩ "
//                        )
//                    }"
//                )
//            }
            ptr += readLen
        }
        else -> -1
    }

    override fun close() {
        readEnd = true
        debugFetchFile("CLOSE", "$id/$src")
        source.close()
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

        val src = request.uri.path.substring(1)

        debugFetchFile("OPEN", src)
        lateinit var dirname: String
        lateinit var filename: String

        src.lastIndexOf('/').also { it ->
            when (it) {
                -1 -> {
                    filename = src
                    dirname = ""
                }
                else -> {
                    filename = src.substring(it + 1)
                    dirname = src.substring(0..it)
                }
            }
            src.substring(0..it)
        }

        /// 尝试打开文件，如果打开失败就走 404 no found 响应
        val filenameList = App.appContext.assets.list(dirname) ?: emptyArray()

        lateinit var response: Response
        if (!filenameList.contains(filename)) {
            debugFetchFile("NO-FOUND", request.uri.path)
            response = Response(Status.NOT_FOUND).body("the file(${request.uri.path}) not found.")
        } else {
            response = Response(status = Status.OK)

            // buffer 模式，就是直接全部读取出来
            // TODO auto 模式就是在通讯次数和单次通讯延迟之间的一个取舍。如果分片次数少于2次，那么就直接发送，没必要分片
            response = if (mode != "stream") {
                /**
                 * 打开一个读取流
                 */
                val assetStream = App.appContext.assets.open(
                    src, AssetManager.ACCESS_BUFFER
                )
                /**
                 * 一次性发送
                 */
                response.body(MemoryBody(assetStream.readByteArray()))
            } else {
                val streamBody = ChunkAssetsFileStream(
                    src, chunkSize = chunk, preReadableSize = if (preRead) chunk else 0
                )
                /**
                 * 将它分片读取
                 */
                response.header("X-Assets-Id", streamBody.id.toString()).body(streamBody)
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
        }

        response
    }.getOrElse {
        Response(Status.INTERNAL_SERVER_ERROR)
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
