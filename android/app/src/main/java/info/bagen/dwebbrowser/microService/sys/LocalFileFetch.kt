package info.bagen.dwebbrowser.microService.sys

import android.content.res.AssetManager
import android.webkit.MimeTypeMap
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.util.APP_DIR_TYPE
import info.bagen.dwebbrowser.util.FilesUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.readByteArray
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.ipc.message.PreReadableInputStream
import org.dweb_browser.microservice.sys.dns.debugFetchFile
import org.dweb_browser.microservice.sys.dns.nativeFetchAdaptersManager
import org.http4k.core.MemoryBody
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

class LocalFileFetch private constructor() {

  companion object {
    val INSTANCE: LocalFileFetch by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
      LocalFileFetch()
    }
  }

  init {
    CoroutineScope(ioAsyncExceptionHandler).launch {
      nativeFetchAdaptersManager.append { fromMM, request ->
        localeFileFetch(fromMM, request)
      }
    }
  }

  private val mimeTypeMap by lazy { MimeTypeMap.getSingleton() }

  /**
   * 安全的 AssetManager 文件读取工具
   */
  private class ChunkAssetsFileStream(
    val src: String,
    val chunkSize: Int = defaultChunkSize,
    override val preReadableSize: Int = chunkSize,
    isSys: Boolean = true
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
      if (isSys) {
        return@lazy App.appContext.assets.open(
          src, AssetManager.ACCESS_STREAMING
        )
      }
      return@lazy File(src).inputStream()
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

  private fun getLocalFetch(remote: MicroModule, request: Request): Response {
    val path = request.uri.path
    val isSys = path.startsWith("/sys/")
    val mode = request.query("mode") ?: "auto"
    val chunk = request.query("chunk")?.toIntOrNull() ?: ChunkAssetsFileStream.defaultChunkSize
    val preRead = request.query("pre-read")?.toBooleanStrictOrNull() ?: false

    var src = if (isSys) {
      request.uri.path.substring(1)
    } else {
      request.uri.path
    }
    // 如果是sys需要移除sys 然后 转发到assets 如果是usr需要转发到data里
    if (path.startsWith("/sys/")) {
      src = src.substring(4)
    }

    debugFetchFile("OPEN-Assets $isSys", "src:$src path:$path")

    lateinit var dirname: String
    lateinit var filename: String
    lateinit var filenameList: Array<String>
    if (isSys) {
      // 读取assets的文件
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
      filenameList = App.appContext.assets.list(dirname) ?: emptyArray()
    } else {
      // 读取app内存的文件
      dirname =
        App.appContext.dataDir.absolutePath + File.separator + APP_DIR_TYPE.SystemApp.rootName + File.separator + remote.mmid
      filename = dirname + src

      filenameList = FilesUtil.traverseFileTree(dirname).toTypedArray()
    }
    debugFetchFile("OPEN-DataSrc", "dirname=$dirname, src=$src")

    lateinit var response: Response
    if (!filenameList.contains(filename)) {
      debugFetchFile("NO-FOUND-Assets", request.uri.path)
      response = Response(Status.NOT_FOUND).body("the file(${request.uri.path}) not found.")
    } else {
      response = Response(status = Status.OK)

      // buffer 模式，就是直接全部读取出来
      // TODO auto 模式就是在通讯次数和单次通讯延迟之间的一个取舍。如果分片次数少于2次，那么就直接发送，没必要分片
      response = if (mode == "stream") {
        val streamBody = if (isSys) {
          ChunkAssetsFileStream(
            src, chunkSize = chunk, preReadableSize = if (preRead) chunk else 0, true
          )
        } else {
          ChunkAssetsFileStream(
            filename, chunkSize = chunk, preReadableSize = if (preRead) chunk else 0, false
          )
        }
        /**
         * 将它分片读取
         */
        response.header("X-Assets-Id", streamBody.id.toString()).body(streamBody)
      } else {
        /**
         * 打开一个读取流
         */
        val assetStream = if (isSys) {
          App.appContext.assets.open(
            src, AssetManager.ACCESS_BUFFER
          )
        } else {
          File(filename).inputStream()
        }
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
      return@runCatching getLocalFetch(remote, request)
    }.getOrElse {
      Response(Status.INTERNAL_SERVER_ERROR)
    }

    else -> null
  }
}