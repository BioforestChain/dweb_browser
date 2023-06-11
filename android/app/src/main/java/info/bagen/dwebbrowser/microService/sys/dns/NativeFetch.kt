package info.bagen.dwebbrowser.microService.sys.dns

import android.content.res.AssetManager
import android.webkit.MimeTypeMap
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.core.MicroModule
import info.bagen.dwebbrowser.microService.helper.AdapterManager
import org.dweb_browser.helper.*
import info.bagen.dwebbrowser.microService.helper.readByteArray
import info.bagen.dwebbrowser.microService.core.ipc.PreReadableInputStream
import info.bagen.dwebbrowser.util.APP_DIR_TYPE
import info.bagen.dwebbrowser.util.FilesUtil
import org.http4k.client.ApacheClient
import org.http4k.core.*
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

typealias FetchAdapter = suspend (remote: MicroModule, request: Request) -> Response?


inline fun debugFetch(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("fetch", tag, msg, err)

inline fun debugFetchFile(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("fetch-file", tag, msg, err)

/**
 *
 * file:/// => /usr & /sys as const
 * file://file.sys.dweb/ => /home & /tmp & /share as userData
 */
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

    override fun read() = source.read()
    override fun available() = chunkSize.coerceAtMost(totalSize - ptr)

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
//                        b.slice(0 until readLen).toByteArray().toByteArray().replace(
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
 * 安全的 AssetManager 文件读取工具
 */
class ChunkDataFileStream(
    val src: String,
    private val chunkSize: Int = defaultChunkSize,
    override val preReadableSize: Int = chunkSize
) : InputStream(), PreReadableInputStream {
    companion object {
        /// 默认1mb切一次
        const val defaultChunkSize = 1024 * 1024
        private var acc_id = AtomicInteger(1)
    }

    val id = acc_id.getAndAdd(1)

    init {
        debugFetchFile("INIT", "$id/$src")
    }

    val source by lazy { File(src).inputStream() }

    var ptr = 0
    private val totalSize by lazy { source.available() }

    override fun read() = source.read()
    override fun available() = chunkSize.coerceAtMost(totalSize - ptr)

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

private fun readUsrFetch(remote: MicroModule, request: Request): Response {
    runCatching {
        val mode = request.query("mode") ?: "auto"
        val chunk = request.query("chunk")?.toIntOrNull() ?: ChunkDataFileStream.defaultChunkSize
        val preRead = request.query("pre-read")?.toBooleanStrictOrNull() ?: false

        val src = request.uri.path
        // 默认是需要 file:///usr/xxx.js
        val dirname: String = App.appContext.dataDir.absolutePath + File.separator +
                APP_DIR_TYPE.SystemApp.rootName + File.separator + remote.mmid
        val filename: String = dirname + src
        debugFetchFile("OPEN-DataSrc", "dirname=$dirname, src=$src")

        val filenameList = FilesUtil.traverseFileTree(dirname)

        lateinit var response: Response
        if (!filenameList.contains(filename)) {
            debugFetchFile("NO-FOUND-DataSrc", request.uri.path + "-->" + filename)
            response = Response(Status.NOT_FOUND).body("the file(${request.uri.path}) not found.")
        } else {
            response = Response(status = Status.OK)

            // buffer 模式，就是直接全部读取出来
            // TODO auto 模式就是在通讯次数和单次通讯延迟之间的一个取舍。如果分片次数少于2次，那么就直接发送，没必要分片
            response = if (mode == "stream") {
                val streamBody = ChunkDataFileStream(
                    filename, chunkSize = chunk, preReadableSize = if (preRead) chunk else 0
                )
                /**
                 * 将它分片读取
                 */
                response.header("X-Assets-Id", streamBody.id.toString()).body(streamBody)
            } else {
                val dataStream = File(filename).inputStream() // 打开一个读取流
                response.body(MemoryBody(dataStream.readByteArray())) // 一次性发送
            }

            /// 尝试加入 Content-Type
            val extension = MimeTypeMap.getFileExtensionFromUrl(request.uri.path)
            if (extension != null) {
                val type = mimeTypeMap.getMimeTypeFromExtension(extension)
                if (type != null) {
                    response = response.header("Content-Type", type)
                }
            }
            return   response
        }
        return response
    }.getOrElse {
        return  Response(Status.INTERNAL_SERVER_ERROR)
    }
}

fun getLocalFetch(remote: MicroModule,request: Request):Response {
    val  path = request.uri.path
    if (path.startsWith("/usr/")) {
        return readUsrFetch(remote,request)
    }
    val mode = request.query("mode") ?: "auto"
    val chunk = request.query("chunk")?.toIntOrNull() ?: ChunkAssetsFileStream.defaultChunkSize
    val preRead = request.query("pre-read")?.toBooleanStrictOrNull() ?: false

    var src = request.uri.path.substring(1)
    // 如果是sys需要移除sys 然后 转发到assets
    if (path.startsWith("/sys/")) {
        src = src.substring(4)
    }

    debugFetchFile("OPEN-Assets", "src:$src path:$path")

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
        debugFetchFile("NO-FOUND-Assets", request.uri.path)
        response = Response(Status.NOT_FOUND).body("the file(${request.uri.path}) not found.")
    } else {
        response = Response(status = Status.OK)

        // buffer 模式，就是直接全部读取出来
        // TODO auto 模式就是在通讯次数和单次通讯延迟之间的一个取舍。如果分片次数少于2次，那么就直接发送，没必要分片
        response = if (mode == "stream") {
            val streamBody = ChunkAssetsFileStream(
                src, chunkSize = chunk, preReadableSize = if (preRead) chunk else 0
            )
            /**
             * 将它分片读取
             */
            response.header("X-Assets-Id", streamBody.id.toString()).body(streamBody)
        } else {
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
        }

        /// 尝试加入 Content-Type
        val extension = MimeTypeMap.getFileExtensionFromUrl(request.uri.path)
        if (extension != null) {
            val type = mimeTypeMap.getMimeTypeFromExtension(extension)
            if (type != null) {
                response = response.header("Content-Type", type)
            }
        }
      return response
    }

   return response
}

/**
 * 加载本地文件
 */
private fun localeFileFetch(remote: MicroModule, request: Request) = when {
    request.uri.scheme == "file" && request.uri.host == "" -> runCatching {
      return@runCatching getLocalFetch(remote,request)
    }.getOrElse {
        Response(Status.INTERNAL_SERVER_ERROR)
    }
    request.uri.scheme == "https" && request.uri.host == "browser.dweb" -> runCatching {
        return@runCatching getLocalFetch(remote,request)
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
