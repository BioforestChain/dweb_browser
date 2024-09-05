package org.dweb_browser.core.std.file

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.http.router.IChannelHandlerContext
import org.dweb_browser.core.http.router.IHandlerContext
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.file.ext.RespondLocalFileContext.Companion.respondLocalFile
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.StringEnumSerializer
import org.dweb_browser.helper.consumeEachCborPacket
import org.dweb_browser.helper.decodeURIComponent
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.pure.crypto.hash.sha256
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.ext.mime
import org.dweb_browser.pure.http.queryAsOrNull
import org.dweb_browser.pure.io.SystemFileSystem
import org.dweb_browser.pure.io.copyTo
import org.dweb_browser.pure.io.toByteReadChannel

val debugFile = Debugger("file")

/**
 * 文件模块属于一种标准，会有很多模块都实现这个标准
 *
 * 比如 视频、相册、音乐、办公 等模块都是对文件读写有刚性依赖的，因此会基于标准文件模块实现同样的标准，这样别的模块可以将同类型的文件存储到它们的文件夹标准下管理
 */
class FileNMM : NativeMicroModule("file.std.dweb", "File Manager") {

  companion object {
    val nativeFileSystem = object : VirtualFsDirectory {
      override fun isMatch(firstSegment: String) = true
      override val fs: FileSystem = SystemFileSystem
      override fun resolveTo(remote: IMicroModuleManifest, virtualFullPath: Path) = virtualFullPath
    }

    internal fun findVfsDirectory(firstSegment: String): VirtualFsDirectory {
      for (adapter in fileTypeAdapterManager.adapters) {
        if (adapter.isMatch(firstSegment)) {
          return adapter
        }
      }
      return nativeFileSystem
    }

    private fun getVirtualFsPath(context: IMicroModuleManifest, virtualPathString: String) =
      VirtualFsPath(context, virtualPathString, ::findVfsDirectory)
  }

  private fun IHandlerContext.getVfsPath(pathKey: String = "path") =
    getVirtualFsPath(ipc.remote, request.query(pathKey))

  private fun IHandlerContext.getPath(pathKey: String = "path") =
    getVfsPath(pathKey).let { Pair(it.fsFullPath, it.fs) }

  private suspend fun IHandlerContext.getPathInfo(fsPath: VirtualFsPath = getVfsPath()): JsonElement {
    val metadata = fsPath.fs.safeMetadataOrNull(fsPath.fsFullPath)
    return if (metadata == null) {
      JsonNull
    } else {
      FileMetadata(
        isFile = metadata.isRegularFile,
        isDirectory = metadata.isDirectory,
        size = metadata.size,
        createdTime = metadata.createdAtMillis ?: 0,
        lastReadTime = metadata.lastAccessedAtMillis ?: 0,
        lastWriteTime = metadata.lastModifiedAtMillis ?: 0
      ).toJsonElement()
    }
  }

  inner class FileRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    init {
      /// 将 `file:///` 请求路由到 file.std.dweb
      nativeFetchAdaptersManager.append(order = 3) { fromMM, request ->
        return@append request.respondLocalFile {
          val fileIpc = fromMM.connect("file.std.dweb")
          fileIpc.request(request)
        }
      }.removeWhen(mmScope)
      /// 提供直接的文件读取
      routesNotFound = {
        /// 为 file:///  请求提供服务
        /// TODO 跟下面的 order = 3.1 合并在一起
        request.respondLocalFile {
          if (request.method == PureMethod.GET) {
            val vfsPath = getVirtualFsPath(ipc.remote, request.url.encodedPath.decodeURIComponent())
            val create = request.queryAsOrNull<Boolean>("create") ?: false
            debugFile("easy-read") {
              "create=$create filepath=${vfsPath.fsFullPath},endCodePath:${request.url.encodedPath}"
            }
            if (create) {
              touchFile(vfsPath.fsFullPath, vfsPath.fs)
            }
            if (vfsPath.fs == ResourceFileSystem.FileSystem) {
              ResourceFileSystem.prepare(vfsPath.fsFullPath).await()
            }
            val size = vfsPath.fs.metadata(vfsPath.fsFullPath).size
            val fileSource = vfsPath.fs.source(vfsPath.fsFullPath).buffer()

            val skip = request.queryAsOrNull<Long>("skip")
            if (skip != null) {
              fileSource.skip(skip)
            }
            returnFile(
              fileSource.toByteReadChannel(mmScope),
              vfsPath.vfsDirectory.getFileHeaders(ipc.remote, vfsPath)
                .apply { size?.also { setContentLength(size) } })
          } else defaultRoutesNotFound()
        } ?: defaultRoutesNotFound()
      }
    }

    fun touchFile(filepath: Path, fs: FileSystem) {
      if (!fs.exists(filepath)) {
        filepath.parent?.let { dirpath ->
          fs.createDirectories(dirpath, false)
        }
        fs.write(filepath, true) {
          write(byteArrayOf())
          this
        }.close()
      }
    }

    private val sysFileSystem = object : VirtualFsDirectory {
      override fun isMatch(firstSegment: String) = firstSegment == "sys"
      override val fs: FileSystem = ResourceFileSystem.FileSystem
      override fun resolveTo(remote: IMicroModuleManifest, virtualFullPath: Path) =
        virtualFullPath - virtualFullPath.first
    }
    private val pickerFileSystem = object : VirtualFsDirectory {
      override fun isMatch(firstSegment: String) = firstSegment == "picker"
      override val fs: FileSystem = PickerFileSystem.FileSystem
      val basePath = "/picker".toPath()
      override fun resolveTo(remote: IMicroModuleManifest, virtualFullPath: Path) = virtualFullPath
      val getPickerFile = PickerFileSystem::getPickerFile
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun _bootstrap() {
      /// file:///data/*
      getDataVirtualFsDirectory().also {
        fileTypeAdapterManager.append(adapter = it).removeWhen(mmScope)
      }
      /// file:///cache/*
      getCacheVirtualFsDirectory().also {
        fileTypeAdapterManager.append(adapter = it).removeWhen(mmScope)
      }
      /// file:///download/*
      getExternalDownloadVirtualFsDirectory().also {
        fileTypeAdapterManager.append(adapter = it).removeWhen(mmScope)
      }

      /// file:///blob/*
      fileTypeAdapterManager.append(adapter = blobReadonlyVirtualFsDirectory).removeWhen(mmScope)

      /// file:///sys/*
      fileTypeAdapterManager.append(adapter = sysFileSystem).removeWhen(mmScope)

      /// file:///picker/*
      fileTypeAdapterManager.append(adapter = pickerFileSystem).removeWhen(mmScope)

      /// nativeFetch 适配 file:///*/** 的读取请求，这里只处理最简单的请求，不带任何参数的哪种
      nativeFetchAdaptersManager.append(order = 3.1f) { fromMM, request ->
        return@append request.respondLocalFile {
          if (request.method == PureMethod.GET && request.url.parameters.isEmpty()) {
            debugFile("read file", "$fromMM => ${request.href}")
            val (_, firstSegment, contentPath) = filePath.split("/", limit = 3)

            val vfsDirectory = findVfsDirectory(firstSegment)
            val vfsPath = VirtualFsPath(fromMM, filePath) { vfsDirectory }
            returnFile(
              vfsPath.fs,
              vfsPath.fsFullPath,
              headers = vfsDirectory.getFileHeaders(fromMM, vfsPath),
              scope = fromMM.getRuntimeScope()
            )
          } else returnNext()
        }
      }.removeWhen(mmScope)

      routes(
        // 使用Duplex打开文件句柄，当这个Duplex关闭的时候，自动释放文件句柄
        "/open" bind PureMethod.GET by defineCborPackageResponse {
          val (filepath, fs) = getPath()
          debugFile("/open", filepath)
          val handler = fs.openReadWrite(filepath)
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
                is FileOpAppend -> handler.appendingSink().buffer().let { bufferedSink ->
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
        "/createDir" bind PureMethod.POST by defineBooleanResponse {
          val (path, fs) = getPath()
          if (fs.exists(path)) {
            fs.safeMetadata(path).isDirectory
          } else {
            fs.createDirectories(path, true)
            true
          }
        },
        // 列出列表
        "/listDir" bind PureMethod.GET by defineJsonResponse {
          val vfsPath = getVfsPath()
          val recursive = request.queryAsOrNull<Boolean>("recursive") ?: false

          val paths = (if (recursive) vfsPath.fs.listRecursively(vfsPath.fsFullPath)
            /// 列出
            .iterator() else vfsPath.fs.list(vfsPath.fsFullPath).iterator())

          buildJsonArray {
            for (path in paths) {
              add(vfsPath.toVirtualPathString(path))
            }
          }
        },
        // 读取文件，一次性读取，可以指定开始位置
        "/read" bind PureMethod.GET by definePureStreamHandler {
          val (filepath, fs) = getPath()
          val create = request.queryAsOrNull<Boolean>("create") ?: false
          debugFile("/read", "create=$create filepath=$filepath")
          if (create) {
            touchFile(filepath, fs)
          }
          val fileSource = fs.safeSource(filepath).buffer()

          val skip = request.queryAsOrNull<Long>("skip")
          if (skip != null) {
            fileSource.skip(skip)
          }
          PureStream(fileSource.toByteReadChannel(mmScope))
        },
        // 写入文件，一次性写入
        "/write" bind PureMethod.POST by defineEmptyResponse {
          val (filepath, fs) = getPath()
          val create = request.queryAsOrNull<Boolean>("create") ?: false

          /**
           * 写入之前备份，避免写入过程中应用中断导致一系列问题，对于文件数据库很有必要
           */
          val backup = request.queryAsOrNull<Boolean>("backup") ?: false

          if (backup) {
            val backupFilepath = "$filepath.${randomUUID()}.bak".toPath()
            touchFile(backupFilepath, fs)
            val backupFileSource = fs.sink(backupFilepath, false).buffer()
            request.body.toPureStream().getReader("write to file").copyTo(backupFileSource)
            if (fs.exists(filepath)) {
              fs.delete(filepath, false)
            }
            fs.atomicMove(backupFilepath, filepath)
          } else {
            if (create) {
              touchFile(filepath, fs)
            }
            val fileSource = fs.sink(filepath, false).buffer()
            request.body.toPureStream().getReader("write to file").copyTo(fileSource)
          }
        },
        // 追加文件，一次性追加
        "/append" bind PureMethod.PUT by defineEmptyResponse {
          val (filepath, fs) = getPath()
          debugFile("/append", filepath)
          val create = request.queryAsOrNull<Boolean>("create") ?: false
          if (create) {
            touchFile(filepath, fs)
          }
          val fileSource = fs.appendingSink(filepath, false).buffer()

          request.body.toPureStream().getReader("write to file").copyTo(fileSource)
        },
        // 路径是否存在
        "/exist" bind PureMethod.GET by defineBooleanResponse {
          val (filepath, fs) = getPath()
          runCatching {
            fs.exists(filepath)
          }.getOrDefault(false)
        },
        // 获取路径的基本信息
        "/info" bind PureMethod.GET by defineJsonResponse {
          getPathInfo()
        },
        "/remove" bind PureMethod.DELETE by defineBooleanResponse {
          val (filepath, fs) = getPath()
          val recursive = request.queryAsOrNull<Boolean>("recursive") ?: false
          runCatching {
            if (recursive) {
              fs.deleteRecursively(filepath, false)
            } else {
              fs.delete(filepath, false)
            }
            true
          }.getOrElse { false }
        },
        "/move" bind PureMethod.GET by defineBooleanResponse {
          val (sourcePath, sourceFs) = getPath("sourcePath")
          val (targetPath, targetFs) = getPath("targetPath")
          debugFile("/move", "sourcePath:$sourcePath => $targetPath")
          (sourceFs == targetFs).trueAlso {
            // 如果不存在则需要创建空文件夹
            if (!targetFs.exists(targetPath)) {
              targetFs.createDirectories(targetPath, true)
            }
            // 需要保证文件夹为空
            targetFs.deleteRecursively(targetPath, false)
            // atomicMove 如果是不同的文件系统时，移动会失败，如 data 目录移动到 外部download目录，所以需要使用copy后自行删除源文件
            sourceFs.atomicMove(sourcePath, targetPath)
          }
        },
        "/copy" bind PureMethod.GET by defineBooleanResponse {
          // 由于 copy 需要保证目标Path的父级节点存在，所以增加判断构建操作
          val (sourcePath, sourceFs) = getPath("sourcePath")
          val (targetPath, targetFs) = getPath("targetPath")
          debugFile("copy", "$sourcePath => $targetPath")
          targetFs.deleteRecursively(targetPath, false) // 先删除，避免拷贝到失败
          targetPath.parent?.let { parentPath ->
            if (!targetFs.exists(parentPath)) {
              targetFs.createDirectories(parentPath, true)
            }
          }
          if (sourceFs == targetFs) {
            sourceFs.copy(sourcePath, targetPath)
          } else {
            /// TODO 要支持文件夹的拷贝
            targetFs.write(targetPath, true) {
              sourceFs.read(sourcePath) {
                this@write.writeAll(this@read.buffer)
              }
            }
          }
          true
        },
        "/watch" byChannel {
          val vfsPath = getVfsPath()
          val recursive = request.queryAsOrNull<Boolean>("recursive") ?: false

          // TODO 开启文件监听，将这个路径添加到监听列表中

          if (request.queryAsOrNull<Boolean>("first") != false) {
            sendPath(FileWatchEventName.First, vfsPath.fsFullPath, vfsPath)
            if (recursive) {
              for (childPath in vfsPath.fs.listRecursively(vfsPath.fsFullPath)) {
                sendPath(FileWatchEventName.First, childPath, vfsPath)
              }
            }
          }
        },
        "/picker" bind PureMethod.GET by defineStringResponse {
          val (realPath, fs) = getPath()
          val name = realPath.name
          val pickerPathString =
            pickerFileSystem.basePath.resolve("${randomUUID()}/${name}").toString()
          PickerFileSystem.files[pickerPathString] = PickerFileSystem.PickerFile(fs, realPath)
          pickerPathString
        },
        "/realPath" bind PureMethod.GET by defineStringResponse {
          var (path, fs) = getPath()
          /// 解除开picker的递归
          while (fs == pickerFileSystem.fs) {
            val pickerFile = pickerFileSystem.getPickerFile(path)
            if (pickerFile != null) {
              path = pickerFile.path
              fs = pickerFile.fs
            }
          }
          path.toString()
        },
        "/blob/create" bind PureMethod.POST by defineStringResponse {
          /// 根据 content-type 来写入数据
          val mime = (request.queryOrNull("mime")
            ?: request.headers.get("Content-Type"))?.let { contentType ->
            ContentType.parse(contentType).mime
          } ?: run {
            // 如果没有提供
            request.queryOrNull("ext")?.let { ext ->
              if (ext.contains(".") || filenameBanChars.any { ext.contains(it) } || ext.length > 64) {
                throwException(HttpStatusCode.Forbidden, "invalid ext: $ext")
              }
              ContentType.parse(ext).mime
            }
          } ?: "application/octet-stream"

          /// 获取文件名算法
          val ag = (request.queryOrNull("ag")?.lowercase() ?: "sha256").let { ag ->
            BlobAlgorithm.ALL[ag] ?: throwException(
              HttpStatusCode.Forbidden,
              "invalid ag: $ag"
            )
          }

          /// 根据算法生成文件名
          val data = request.body.toPureBinary()
          val vPath = when (ag) {
            BlobAlgorithm.Sha256 -> sha256(data).toHexString()
              .let { hash -> "/blob/v1/$mime/$hash" }
          }

          val vfsPath = blobReadWriteVirtualFsDirectory.resolveTo(ipc.remote, vPath.toPath())
          touchFile(vfsPath, blobReadWriteVirtualFsDirectory.fs)
          val fileSource = blobReadWriteVirtualFsDirectory.fs.sink(vfsPath, false).buffer()
          fileSource.write(data)
          fileSource.flush()
          fileSource.close()

          "file://$vPath".also { debugFile("create/blob", it) }
        },
      )
    }

    private val filenameBanChars = setOf('/', '"', '*', '<', '>', '?', '\\', '|', ':')

    override suspend fun _shutdown() {
    }

  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = FileRuntime(bootstrapContext)

}

enum class BlobAlgorithm(val ag: String) {
  Sha256("sha256"),
  ;

  companion object {
    val ALL = entries.associateBy { it.ag }
  }
}


object FileWatchEventNameSerializer : StringEnumSerializer<FileWatchEventName>(
  "FileWatchEventName",
  FileWatchEventName.ALL_VALUES,
  { eventName })

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
  UnlinkDir("unlinkDir"),
  ;

  companion object {
    val ALL_VALUES = entries.associateBy { it.eventName }
  }
}

data class FileWatchEvent(
  val type: FileWatchEventName,
  val path: String,
)

suspend fun IChannelHandlerContext.sendPath(
  type: FileWatchEventName, path: Path, vfsPath: VirtualFsPath,
) {
  pureChannelContext.sendJsonLine(
    FileWatchEvent(
      type,
      vfsPath.toVirtualPathString(path),
    )
  )
}