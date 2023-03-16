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
inline fun debugLocaleFile(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("locale-file", tag, msg, err)


val nativeFetchAdaptersManager = AdapterManager<FetchAdapter>()

private val mimeTypeMap by lazy { MimeTypeMap.getSingleton() }


/**
 * 安全的 AssetManager 文件读取工具
 */
class ChunkAssetsFileStream(
    val src: String,
    val chunkSize: Int = defaultChunkSize,
    override val preReadableSize: Int = chunkSize
) :
    InputStream(), PreReadableInputStream {
    companion object {
        /// 默认1mb切一次
        val defaultChunkSize = 1024 * 1024
        private var acc_id = 1
    }

    val id = acc_id++

    init {
        debugLocaleFile("INIT","$id/$src")
    }

    val source by lazy {
        App.appContext.assets.open(
            src,
            AssetManager.ACCESS_STREAMING
        )
    }

    var ptr = 0
    val totalSize by lazy { source.available() }

    override inline fun read() = source.read()
    override inline fun available() = chunkSize.coerceAtMost(totalSize - ptr)

    var readEnd = false

    @Synchronized
    override fun read(b: ByteArray?, off: Int, len: Int) = when (readEnd) {
        false -> source.read(b, off, len).also { readLen ->
            if (readLen != len) {
                debugLocaleFile("READ","$id/$src($totalSize) $off+$len~>$readLen")
            }
            if (readLen == -1) {
                readEnd = true
            }
            ptr += readLen
        }
        else -> -1
    }

    override fun close() {
        readEnd = true
        debugLocaleFile("CLOSE","$id/$src")
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

        debugLocaleFile("OPEN", src)
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
        val tryOpen = App.appContext.assets.list(dirname) ?: emptyArray()
        debugLocaleFile("LIST","$dirname:\n\t${tryOpen.joinToString("\n\t")}")
        if (!tryOpen.contains(filename)) {
            throw Exception("No Found")
        }
        var response = Response(status = Status.OK)//.header("Content-Length", totalSize.toString())

        response = if (
        // buffer 模式，就是直接全部读取出来
        // TODO auto 模式就是在通讯次数和单次通讯延迟之间的一个取舍。如果分片次数少于2次，那么就直接发送，没必要分片
        mode != "stream"
        ) {
            /**
             * 打开一个读取流
             */
            val assetStream = App.appContext.assets.open(
                src,
                AssetManager.ACCESS_BUFFER
            )
            /**
             * 一次性发送
             */
            response.body(MemoryBody(assetStream.readByteArray()))
        } else {
            /**
             * 将它分片读取
             */
            response.body(
                ChunkAssetsFileStream(
                    src,
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
        debugLocaleFile("NO-FOUND", request.uri.path, it)
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
