package info.bagen.dwebbrowser.microService.sys

import android.content.res.AssetManager
import info.bagen.dwebbrowser.App
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fromFilePath
import io.ktor.util.cio.toByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.util.APP_DIR_TYPE
import org.dweb_browser.browserUI.util.FilesUtil
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.http.PureRequest
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStringBody
import org.dweb_browser.microservice.ipc.helper.PreReadableInputStream
import org.dweb_browser.microservice.sys.dns.debugFetch
import org.dweb_browser.microservice.sys.dns.debugFetchFile
import org.dweb_browser.microservice.sys.dns.nativeFetchAdaptersManager
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
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
        if (request.url.protocol.name == "file" && request.url.host == "") {
          debugFetch("LocalFile/nativeFetch", "$fromMM => ${request.href}")
          runCatching {
            return@runCatching getLocalFetch(fromMM, request)
          }.getOrElse {
            PureResponse(HttpStatusCode.InternalServerError)
          }
        } else null
      }
    }
  }

  /**
   * 安全的 AssetManager 文件读取工具
   */
  private class ChunkAssetsFileStream(
    val src: String,
    val chunkSize: Int = defaultChunkSize,
    override val preReadableSize: Int = chunkSize,
    isSys: Boolean = true
  ) : ByteChannel, PreReadableInputStream {
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
        ).toByteReadChannel()
      }

      return@lazy File(src).inputStream().toByteReadChannel()
    }

    var ptr = 0
    val totalSize by lazy { source.availableForRead }
    override fun close() {
      debugFetchFile("CLOSE", "$id/$src")
      source.cancel(null)
    }

    override fun isOpen(): Boolean {
      TODO("Not yet implemented")
    }

    override fun read(p0: ByteBuffer?): Int {
      TODO("Not yet implemented")
    }

    override fun write(p0: ByteBuffer?): Int {
      TODO("Not yet implemented")
    }
//
//    override fun read() = source.read()
//    override fun available() = chunkSize.coerceAtMost(totalSize - ptr)
//
//    var readEnd = false
//
//    @Synchronized
//    override fun read(b: ByteArray, off: Int, len: Int) = when (readEnd) {
//      false -> source.read(b, off, len).also { readLen ->
//        if (readLen != len) {
//          // assets 的流内容是明确的，所以一般不会有 available 突变的问题，除非被重写
//          readEnd = true
//          debugFetchFile("READ", "$id/$src($totalSize) $off+$len~>$readLen")
//        }
//        if (readLen == -1) {
//          readEnd = true
//        }
//        ptr += readLen
//      }
//
//      else -> -1
//    }
//
//    override fun close() {
//      readEnd = true
//      debugFetchFile("CLOSE", "$id/$src")
//      source.close()
//    }
  }

  enum class PathType(val type: String, val dirName: String, val tag: String) {
    SYS(type = "/sys/", dirName = "", "LocalFetch > Assets"),
    ICONS(
      type = "/local_icons/",
      dirName = App.appContext.filesDir.absolutePath + "/icons",
      "LocalFetch > Tmp"
    ),
    OTHER(
      type = "/",
      dirName = App.appContext.dataDir.absolutePath + File.separator + APP_DIR_TYPE.SystemApp.rootName,
      "LocalFetch > DataSrc"
    ),
    ;
  }

  private fun String.checkPathType() = if (startsWith(PathType.SYS.type)) {
    PathType.SYS
  } else if (startsWith(PathType.ICONS.type)) {
    PathType.ICONS
  } else PathType.OTHER

  private fun getLocalFetch(remote: MicroModule, request: PureRequest): PureResponse {
    val mode = request.query("mode") ?: "auto"
    val chunk = request.query("chunk")?.toIntOrNull() ?: ChunkAssetsFileStream.defaultChunkSize
    val preRead = request.query("pre-read")?.toBooleanStrictOrNull() ?: false
    val path = request.url.encodedPath
    val pathType = path.checkPathType()// path.startsWith("/sys/")

    lateinit var filePath: String
    val src = path.replaceFirst(pathType.type, "")
    val filenameList = when (pathType) {
      PathType.SYS -> {
        // 读取assets的文件
        val dirname = src.lastIndexOf('/').let {
          when (it) {
            -1 -> {
              filePath = src
              ""
            }

            else -> {
              filePath = src.substring(it + 1)
              src.substring(0..it)
            }
          }
        }
        /// 尝试打开文件，如果打开失败就走 404 no found 响应
        App.appContext.assets.list(dirname) ?: emptyArray()
      }

      PathType.ICONS -> {
        // 读取tmp文件
        filePath = pathType.dirName + File.separator + src
        FilesUtil.traverseFileTree(pathType.dirName).toTypedArray()
      }

      else -> {
        // 读取应用内的文件
        filePath = pathType.dirName + "/${remote.mmid}/" + src
        FilesUtil.traverseFileTree(pathType.dirName + "/${remote.mmid}").toTypedArray()
      }
    }
    val tag = pathType.tag
    debugFetchFile(tag, "dirname=${pathType.dirName}, filename=$filePath, path=$path")

    lateinit var response: PureResponse
    if (!filenameList.contains(filePath)) {
      debugFetchFile(tag, "NO-FOUND-File $filePath")
      response = PureResponse(
        HttpStatusCode.NotFound, body = PureStringBody("the file(${request.href}) not found.")
      )
    } else {
      response = PureResponse(HttpStatusCode.OK)

      // buffer 模式，就是直接全部读取出来
      // TODO auto 模式就是在通讯次数和单次通讯延迟之间的一个取舍。如果分片次数少于2次，那么就直接发送，没必要分片
      response = if (mode == "stream") {
        val streamBody = when (pathType) {
          PathType.SYS -> {
            App.appContext.assets.open(
              src, AssetManager.ACCESS_STREAMING
            ).toByteReadChannel()
          }

          else -> {
            File(src).inputStream().toByteReadChannel()
          }
        }
        /// 将它分片读取
//        response.headers.apply { set("X-Assets-Id", streamBody.id.toString()) }
//        response.body = PureStreamBody(streamBody)
//        response.header("X-Assets-Id", streamBody.id.toString()).body(streamBody)

        response.body(streamBody)
      } else {
        /// 打开一个读取流
        val assetStream = when (pathType) {
          PathType.SYS -> App.appContext.assets.open(src, AssetManager.ACCESS_BUFFER)
            .toByteReadChannel()

          else -> File(filePath).inputStream().toByteReadChannel()
        }
        /// 一次性发送
        response.body(assetStream)
      }
    }
    /// 尝试加入 Content-Type
    val extension = ContentType.fromFilePath(filePath)
    if (extension.isNotEmpty()) {
      response.headers.init("Content-Type", extension.first().toString())
    }
    return response
  }
}