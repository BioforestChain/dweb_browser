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
class FileNMM : NativeMicroModule("file.std.dweb", "File Manager") {
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

    fun HandlerContext.getRootAndPath(): Pair<Path, Path> {
      val filepath = request.queryOrNull("path")
      val fileDirectory = findFileDirectory(request.queryOrNull("type"))
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

    fun HandlerContext.getPath(): Path {
      return getRootAndPath().second
    }

    fun HandlerContext.getFileInfo(): JsonElement {
      val metadata = SystemFileSystem.metadataOrNull(getPath());
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
      // 列出列表
      "/list" bind HttpMethod.Get to defineJsonResponse {
        val (root, path) = getRootAndPath()
        val recursively = request.queryAsOrNull<Boolean>("all") ?: false

        (if (recursively) SystemFileSystem.listRecursively(path)
          .toList() else SystemFileSystem.list(path))
          .map {
            it.relativeTo(root).toString()
          }.toJsonElement()
      },
      // 顺序读取文件，一次性读取，但是可以指定开始位置
      "/read" bind HttpMethod.Get to definePureStreamHandler {
        val fileSource = SystemFileSystem.source(getPath()).buffer()

        val skip = request.queryAsOrNull<Long>("skip")
        if (skip != null) {
          fileSource.skip(skip)
        }
        PureStream(fileSource.toByteReadChannel(ioAsyncScope))
      },
      // 顺序写入文件，一次性写入，但是可以用追加的方式
      "/write" bind HttpMethod.Post to defineEmptyResponse {
        val fileSource =
          SystemFileSystem.sink(getPath(), request.queryAsOrNull("append") ?: false).buffer()

        request.body.toPureStream().getReader("write to file").copyTo(fileSource)
      },
      // 文件是否存在
      "/exist" bind HttpMethod.Get to defineBooleanResponse {
        SystemFileSystem.exists(getPath())
      },
      // 获取路径的基本信息
      "/info" bind HttpMethod.Get to defineJsonResponse {
        getFileInfo()
      },
      "/remove" bind HttpMethod.Delete to defineBooleanResponse {
        try {
          SystemFileSystem.deleteRecursively(getPath(), false)
          true
        } catch (e: Throwable) {
          false
        }
      }
//      "/watch" bind HttpMethod.Get to defineJsonLineResponse {
//        emit(getFileInfo())
////        SystemFileSystem.
//
//        val watcher = FileWatcher
//      }
    )
  }

  override suspend fun _shutdown() {
  }

}