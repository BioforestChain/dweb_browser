package org.dweb_browser.core.std.file

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.http.PureStream
import org.dweb_browser.core.http.router.IHandlerContext
import org.dweb_browser.core.http.router.ResponseException
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.file.ext.RespondLocalFileContext.Companion.respondLocalFile
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.StringEnumSerializer
import org.dweb_browser.helper.SystemFileSystem
import org.dweb_browser.helper.consumeEachCborPacket
import org.dweb_browser.helper.copyTo
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.helper.toByteReadChannel
import org.dweb_browser.helper.toJsonElement
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

val debugFile = Debugger("file")

/**
 * 文件模块属于一种标准，会有很多模块都实现这个标准
 *
 * 比如 视频、相册、音乐、办公 等模块都是对文件读写有刚性依赖的，因此会基于标准文件模块实现同样的标准，这样别的模块可以将同类型的文件存储到它们的文件夹标准下管理
 */
class FileNMM : NativeMicroModule("file.std.dweb", "File Manager") {

  init {
    dweb_protocols = listOf("window.std.dweb")
  }

  companion object {
    internal fun findVfsDirectory(firstSegment: String): IVirtualFsDirectory? {
      for (adapter in fileTypeAdapterManager.adapters) {
        if (adapter.isMatch(firstSegment)) {
          return adapter
        }
      }
      return null
    }

    /// TODO 这个函数给出来是给内部使用的
    private fun getVirtualFsPath(context: IMicroModuleManifest, virtualPathString: String) =
      VirtualFsPath(context, virtualPathString, ::findVfsDirectory)

    /**
     * 用于picker映射真实路径
     */
    private val pickerPathToRealPathMap = mutableMapOf<String, Path>()
  }


  /**
   * 虚拟的路径映射
   */
  class VirtualFsPath(
    context: IMicroModuleManifest,
    virtualPathString: String,
    findVfsDirectory: (firstSegment: String) -> IVirtualFsDirectory?
  ) {
    private val virtualFullPath = virtualPathString.toPath(true).also {
      if (!it.isAbsolute) {
        throw ResponseException(
          HttpStatusCode.BadRequest,
          "File path should be absolute path"
        )
      }
      if (it.segments.isEmpty()) {
        throw ResponseException(
          HttpStatusCode.BadRequest, "File path required one segment at least"
        )
      }
    }
    private val virtualFirstSegment = virtualFullPath.segments.first()
    private val virtualFirstPath = "/$virtualFirstSegment".toPath()
    private val virtualContentPath = virtualFullPath.relativeTo(virtualFirstPath)

    private val vfsDirectory = findVfsDirectory(virtualFirstSegment) ?: throw ResponseException(
      HttpStatusCode.NotFound, "No found top-folder: $virtualFirstSegment"
    )
    private val fsBasePath = vfsDirectory.getFsBasePath(context)
    val fsFullPath = fsBasePath.resolve(virtualContentPath)

    fun toVirtualPath(fsPath: Path) = virtualFirstPath.resolve(fsPath.relativeTo(fsBasePath))
    fun toVirtualPathString(fsPath: Path) = toVirtualPath(fsPath).toString()
  }

  fun IHandlerContext.getVfsPath(
    pathKey: String = "path",
  ) = getVirtualFsPath(
    ipc.remote,
    request.query(pathKey)
  )

  fun IHandlerContext.getPath(pathKey: String = "path"): Path {
    val virtualPath = request.query(pathKey)
    if (virtualPath.startsWith("/picker")) {
      return pickerPathToRealPathMap[virtualPath]!!
    }
    return getVfsPath(pathKey).fsFullPath
  }

  fun IHandlerContext.getPathInfo(path: Path = getPath()): JsonElement {
    val metadata = SystemFileSystem.metadataOrNull(path);
    return if (metadata == null) {
      JsonNull
    } else {

      FileMetadata(
        metadata.isRegularFile,
        metadata.isDirectory,
        metadata.size,
        metadata.createdAtMillis ?: 0,
        metadata.lastAccessedAtMillis ?: 0,
        metadata.lastModifiedAtMillis ?: 0
      ).toJsonElement()
    }
  }

  @OptIn(ExperimentalResourceApi::class)
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    getDataVirtualFsDirectory().also {
      fileTypeAdapterManager.append(adapter = it).removeWhen(onAfterShutdown)
    }
    getCacheVirtualFsDirectory().also {
      fileTypeAdapterManager.append(adapter = it).removeWhen(onAfterShutdown)
    }
    /// nativeFetch 适配 file:///*/** 的请求
    nativeFetchAdaptersManager.append { fromMM, request ->
      return@append request.respondLocalFile {
        debugFile("read file", "$fromMM => ${request.href}")
        val (_, firstSegment, contentPath) = filePath.split("/", limit = 3)
        // TODO 未来多平台下，sys的提供由 resource 函数统一供给
        if (firstSegment == "sys") {
          return@respondLocalFile try {
            returnFile(resource(contentPath).readBytes())
          } catch (e: Throwable) {
            /// 终止，不继续尝试从其它地方读取文件
            returnNoFound(e.message)
          }
        } else if (firstSegment == "picker") {
          return@respondLocalFile returnFile(
            SystemFileSystem.source(
              pickerPathToRealPathMap[request.url.encodedPath]!!
            ).buffer().toByteReadChannel()
          )
        }
        return@respondLocalFile when (val vfsDirectory = findVfsDirectory(firstSegment)) {
          null -> returnNext()
          else -> {
            val vfsPath = VirtualFsPath(fromMM, filePath) { vfsDirectory }
            returnFile(
              SystemFileSystem.source(vfsPath.fsFullPath).buffer()
                .toByteReadChannel(fromMM.ioAsyncScope)
            )
          }
        }
      }
    }.removeWhen(onAfterShutdown)

    fun touchFile(filepath: Path) {
      if (!SystemFileSystem.exists(filepath)) {
        SystemFileSystem.createDirectories(filepath.resolve("..", true), false)
        SystemFileSystem.sink(filepath, true).close()
      }
    }
    routes(
      // 使用Duplex打开文件句柄，当这个Duplex关闭的时候，自动释放文件句柄
      "/open" bind HttpMethod.Get to defineCborPackageResponse {
        val handler = SystemFileSystem.openReadWrite(getPath())
        // TODO 这里需要定义完整的操作指令
        request.body.toPureStream().getReader("open file")
          .consumeEachCborPacket<FileOp<*>> { op ->
            when (op) {
              // 获取大小
              is FileOpSize -> emit(handler.size())
              // 读取内容
              is FileOpRead -> emit(when (val fileOffset = op.input.first) {
                null -> handler.source()
                else -> handler.source(fileOffset)
              }.buffer().let { bufferedSource ->
                when (val byteCount = op.input.second) {
                  null -> bufferedSource.readByteArray()
                  else -> bufferedSource.readByteArray(byteCount)
                }.also { bufferedSource.close() }
              })
              // 写入内容
              is FileOpWrite -> emit(when (val fileOffset = op.input.first) {
                null -> handler.sink()
                else -> handler.sink(fileOffset)
              }.buffer().let { bufferedSink ->
                bufferedSink.write(op.input.second)
                bufferedSink.close()
              })
              // 追加内容
              is FileOpAppend -> handler.appendingSink().buffer()
                .let { bufferedSink ->
                  bufferedSink.write(op.input)
                  bufferedSink.close()
                }
              // 主动关闭
              is FileOpClose -> this@consumeEachCborPacket.breakLoop()
            }
          }
        // 关闭句柄
        handler.close()
        // 结束算工流
        end()
      },
      // 创建文件夹
      "/createDir" bind HttpMethod.Post to defineBooleanResponse {
        SystemFileSystem.createDirectories(getPath(), true)
        true
      },
      // 列出列表
      "/listDir" bind HttpMethod.Get to defineJsonResponse {
        val vfsPath = getVfsPath()
        val recursive = request.queryAsOrNull<Boolean>("recursive") ?: false

        val paths = (if (recursive) SystemFileSystem.listRecursively(vfsPath.fsFullPath)
          /// 列出
          .iterator() else SystemFileSystem.list(vfsPath.fsFullPath).iterator());

        buildJsonArray {
          for (path in paths) {
            add(vfsPath.toVirtualPathString(path))
          }
        }
      },
      // 读取文件，一次性读取，可以指定开始位置
      "/read" bind HttpMethod.Get to definePureStreamHandler {
        val filepath = getPath()
        debugFile("/read", filepath)
        val create = request.queryAsOrNull<Boolean>("create") ?: false
        if (create) {
          touchFile(filepath)
        }
        val fileSource = SystemFileSystem.source(filepath).buffer()

        val skip = request.queryAsOrNull<Long>("skip")
        if (skip != null) {
          fileSource.skip(skip)
        }
        PureStream(fileSource.toByteReadChannel(ioAsyncScope))
      },
      // 写入文件，一次性写入
      "/write" bind HttpMethod.Post to defineEmptyResponse {
        val filepath = getPath()
        debugFile("/write", filepath)
        val create = request.queryAsOrNull<Boolean>("create") ?: false
        if (create) {
          touchFile(filepath)
        }
        val fileSource = SystemFileSystem.sink(filepath, false).buffer()

        request.body.toPureStream().getReader("write to file").copyTo(fileSource)
      },
      // 追加文件，一次性追加
      "/append" bind HttpMethod.Put to defineEmptyResponse {
        val filepath = getPath()
        debugFile("/append", filepath)
        val create = request.queryAsOrNull<Boolean>("create") ?: false
        if (create) {
          touchFile(filepath)
        }
        val fileSource = SystemFileSystem.appendingSink(filepath, false).buffer()

        request.body.toPureStream().getReader("write to file").copyTo(fileSource)
      },
      // 路径是否存在
      "/exist" bind HttpMethod.Get to defineBooleanResponse {
        SystemFileSystem.exists(getPath())
      },
      // 获取路径的基本信息
      "/info" bind HttpMethod.Get to defineJsonResponse {
        getPathInfo()
      },
      "/remove" bind HttpMethod.Delete to defineBooleanResponse {
        val recursive = request.queryAsOrNull<Boolean>("recursive") ?: false
        if (recursive) {
          SystemFileSystem.deleteRecursively(getPath(), false)
        } else {
          SystemFileSystem.delete(getPath(), false)
        }
        true
      },
      "/move" bind HttpMethod.Get to defineBooleanResponse {
        val sourcePath = getPath("sourcePath")
        val targetPath = getPath("targetPath")
        debugFile("/move", "sourcePath:$sourcePath => $targetPath")
        // 如果不存在则需要创建空文件夹
        if (!SystemFileSystem.exists(targetPath)) {
          SystemFileSystem.createDirectories(targetPath, true)
        }else {
          // 需要保证文件夹为空
          SystemFileSystem.deleteRecursively(targetPath, false)
        }
        SystemFileSystem.atomicMove(sourcePath, targetPath)
        true
      },
      "/copy" bind HttpMethod.Post to defineBooleanResponse {
        SystemFileSystem.copy(
          getPath("sourcePath"), getPath("targetPath")
        )
        true
      },
      "/watch" bind HttpMethod.Get to defineJsonLineResponse {
        val vfsPath = getVfsPath()
        val recursive = request.queryAsOrNull<Boolean>("recursive") ?: false

        // TODO 开启文件监听，将这个路径添加到监听列表中

        if (request.queryAsOrNull<Boolean>("first") != false) {
          emitPath(FileWatchEventName.First, vfsPath.fsFullPath, vfsPath);
          if (recursive) {
            for (childPath in SystemFileSystem.listRecursively(vfsPath.fsFullPath)) {
              emitPath(FileWatchEventName.First, childPath, vfsPath)
            }
          }
        }
      },
      "/picker" bind HttpMethod.Get to defineStringResponse {
        val realPath = getPath()
        val name = realPath.name
        val pickerPathString = "/picker/${randomUUID()}/${name}"
        pickerPathToRealPathMap[pickerPathString] = realPath
        pickerPathString
      },
      "/realPath" bind HttpMethod.Get to defineStringResponse {
        return@defineStringResponse getPath().toString()
      },
    )
  }

  override suspend fun _shutdown() {
  }

  object FileWatchEventNameSerializer : StringEnumSerializer<FileWatchEventName>(
    "FileWatchEventName",
    FileWatchEventName.ALL_VALUES,
    { eventName });
  @Serializable(with = FileWatchEventNameSerializer::class)
  enum class FileWatchEventName(val eventName: String) {
    /** 初始化监听时，执行的触发 */
    First("first"),

    /** 路径被实例化成文件 */
    Add("add"),

    /** 路径被实例化成文件夹 */
    AddDir("addDir"),

    /** 文件被执行写入或追加 */
    Change("change"),

    /** 文件被移除 */
    Unlink("unlink"),

    /** 文件夹被移除*/
    UnlinkDir("unlinkDir"), ;

    companion object {
      val ALL_VALUES = entries.associateBy { it.eventName }
    }
  }

  data class FileWatchEvent(
    val type: FileWatchEventName,
    val path: String,
  )

  suspend fun JsonLineHandlerContext.emitPath(
    type: FileWatchEventName, path: Path, vfsPath: VirtualFsPath
  ) {
    emit(
      FileWatchEvent(
        type,
        vfsPath.toVirtualPathString(path),
      )
    )
  }
}