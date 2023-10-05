package org.dweb_browser.microservice.std.file

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.dweb_browser.helper.StringEnumSerializer
import org.dweb_browser.helper.SystemFileSystem
import org.dweb_browser.helper.copyTo
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.helper.toByteReadChannel
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.http.PureStream
import org.dweb_browser.microservice.http.bind

/**
 * 文件模块属于一种标准，会有很多模块都实现这个标准
 *
 * 比如 视频、相册、音乐、办公 等模块都是对文件读写有刚性依赖的，因此会基于标准文件模块实现同样的标准，这样别的模块可以将同类型的文件存储到它们的文件夹标准下管理
 */
class FileNMM(serialName: String) : NativeMicroModule("file.std.dweb", "File Manager") {
  private fun findFileDirectory(type: String?): FileDirectory {
    for (adapter in fileTypeAdapterManager.adapters) {
      if (adapter.isMatch(type)) {
        return adapter
      }
    }
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    var lockIdAcc by atomic(0);
    val openFileSourceMap = mutableMapOf<String, Mutex>()
    fileTypeAdapterManager.append(adapter = getDataFileDirectory()).removeWhen(onAfterShutdown)
    fileTypeAdapterManager.append(adapter = getDacheFileDirectory()).removeWhen(onAfterShutdown)

    fun IHandlerContext.getRootAndPath(
      pathKey: String = "path",
      typeKey: String = "type"
    ): Pair<Path, Path> {
      val filepath = request.queryOrNull(pathKey)
      val fileDirectory = findFileDirectory(request.queryOrNull(typeKey))
      val baseDir = fileDirectory.getDir(ipc.remote)

      if (filepath.isNullOrBlank()) {
        return Pair(baseDir, baseDir)
      }

      if (filepath.startsWith("..") || filepath.startsWith('/')) {
        throwException(HttpStatusCode.BadRequest, "File path should by relative path")
      }
      val fullPath = baseDir.relativeTo(filepath.toPath())
      if (!SystemFileSystem.exists(fullPath)) {
        throwException(HttpStatusCode.NotFound, "No found file: $fullPath")
      }
      return Pair(baseDir, fullPath)
    }

    fun IHandlerContext.getPath(pathKey: String = "path", typeKey: String = "type"): Path {
      return getRootAndPath(pathKey, typeKey).second
    }

    fun IHandlerContext.getPathInfo(path: Path = getPath()): JsonElement {
      val metadata = SystemFileSystem.metadataOrNull(path);
      return if (metadata == null) {
        JsonNull
      } else {
        @Serializable
        data class FileMetadata(
          val isFile: Boolean,
          val isDirectory: Boolean,
          val size: Long? = null,
          val createdTime: Long = 0,
          val lastReadTime: Long = 0,
          val lastWriteTime: Long = 0,
        )

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

    routes(
      // 创建文件夹
      "/createDir" bind HttpMethod.Post to defineBooleanResponse {
        SystemFileSystem.createDirectories(getPath(), true)
        true
      },
      // 列出列表
      "/listDir" bind HttpMethod.Get to defineJsonResponse {
        val (root, path) = getRootAndPath()
        val recursive = request.queryAsOrNull<Boolean>("recursive") ?: false

        (if (recursive) SystemFileSystem.listRecursively(path)
          .toList() else SystemFileSystem.list(path))
          .map {
            it.relativeTo(root).toString()
          }.toJsonElement()
      },
      // 读取文件，一次性读取，可以指定开始位置
      "/read" bind HttpMethod.Get to definePureStreamHandler {
        val fileSource = SystemFileSystem.source(getPath()).buffer()

        val skip = request.queryAsOrNull<Long>("skip")
        if (skip != null) {
          fileSource.skip(skip)
        }
        PureStream(fileSource.toByteReadChannel(ioAsyncScope))
      },
      // 写入文件，一次性写入
      "/write" bind HttpMethod.Post to defineEmptyResponse {
        val fileSource =
          SystemFileSystem.sink(getPath(), request.queryAsOrNull("create") ?: false).buffer()

        request.body.toPureStream().getReader("write to file").copyTo(fileSource)
      },
      // 追加文件，一次性追加
      "/append" bind HttpMethod.Put to defineEmptyResponse {
        val fileSource =
          SystemFileSystem.appendingSink(getPath(), request.queryAsOrNull("create") ?: false)
            .buffer()

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
      "/move" bind HttpMethod.Put to defineBooleanResponse {
        SystemFileSystem.atomicMove(
          getPath("sourcePath", "sourceType"),
          getPath("targetPath", "targetType")
        )
        true
      },
      "/watch" bind HttpMethod.Get to defineJsonLineResponse {
        val (root, path) = getRootAndPath()
        val recursive = request.queryAsOrNull<Boolean>("recursive") ?: false

        // TODO 开启文件监听，将这个路径添加到监听列表中

        if (request.queryAsOrNull<Boolean>("first") != false) {
          emitPath(FileWatchEventName.First, path, root);
          if (recursive) {
            for (childPath in SystemFileSystem.listRecursively(path)) {
              emitPath(FileWatchEventName.First, childPath, root)
            }
          }
        }
      },
    )
  }

  override suspend fun _shutdown() {
  }

  object FileWatchEventNameSerializer :
    StringEnumSerializer<FileWatchEventName>(
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
    UnlinkDir("unlinkDir"),
    ;

    companion object {
      val ALL_VALUES = entries.associateBy { it.eventName }
    }
  }

  data class FileWatchEvent(
    val type: FileWatchEventName,
    val path: String,
//    val exists: Boolean,
//    val isFile: Boolean,
//    val isDirectory: Boolean,
  )

  suspend fun JsonLineHandlerContext.emitPath(type: FileWatchEventName, path: Path, root: Path) {
//    val metadata = SystemFileSystem.metadataOrNull(path)
    emit(
      FileWatchEvent(
        type,
        path.relativeTo(root).toString(),
//        metadata != null,
//        metadata?.isRegularFile ?: false,
//        metadata?.isDirectory ?: false,
      )
    )
  }
}