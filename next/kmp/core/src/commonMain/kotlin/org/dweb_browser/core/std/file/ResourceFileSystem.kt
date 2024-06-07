package org.dweb_browser.core.std.file

import dweb_browser_kmp.core.generated.resources.Res
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okio.FileHandle
import okio.FileMetadata
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Sink
import okio.Source
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.globalIoScope
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
object ResourceFileSystem {
  private val fakeFileSystem = FakeFileSystem()
  private val fsScope = globalIoScope
  private val sinkMap = SafeHashMap<String, Deferred<Boolean>>()
  fun prepare(path: Path) = sinkMap.getOrPut(path.toString()) {
    fsScope.async(start = CoroutineStart.UNDISPATCHED) {
      try {
        path.parent?.also {
          fakeFileSystem.createDirectories(it, false)
        }
        val sink = fakeFileSystem.sink(path, true)

        val content = Res.readBytes("files/$path")
        sink.buffer().apply {
          write(content)
          close()
        }
        sink.close()
        true
      } catch (_: Throwable) {
        false
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun <T> Deferred<T>.blockingAwait() = if (isCompleted) {
    getCompleted()
  } else {
    WARNING("blockingAwait-start")
    try {
      runBlocking { await() }
    } finally {
      WARNING("blockingAwait-end")
    }
  }

  val FileSystem = object : ForwardingFileSystem(fakeFileSystem) {

    override fun metadataOrNull(path: Path): FileMetadata? {
      prepare(path).blockingAwait()
      return super.metadataOrNull(path)
    }

    override fun source(file: Path): Source {
      prepare(file).blockingAwait()
      return super.source(file)
    }

    override fun openReadOnly(file: Path): FileHandle {
      prepare(file).blockingAwait()
      return super.openReadOnly(file)
    }

    override fun appendingSink(file: Path, mustExist: Boolean): Sink {
      throw okio.IOException("resources is readonly, not allow appendingSink")
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
      throw okio.IOException("resources is readonly, not allow sink")
    }

    override fun atomicMove(source: Path, target: Path) {
      throw okio.IOException("resources is readonly, not allow atomicMove")
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean) {
      throw okio.IOException("resources is readonly, not allow createDirectory")
    }

    override fun copy(source: Path, target: Path) {
      throw okio.IOException("resources is readonly, not allow copy")
    }

    override fun createSymlink(source: Path, target: Path) {
      throw okio.IOException("resources is readonly, not allow createSymlink")
    }

    override fun deleteRecursively(fileOrDirectory: Path, mustExist: Boolean) {
      throw okio.IOException("resources is readonly, not allow deleteRecursively")
    }

    override fun delete(path: Path, mustExist: Boolean) {
      throw okio.IOException("resources is readonly, not allow delete")
    }

    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle {
      throw okio.IOException("resources is readonly, not allow openReadWrite")
    }

  }
}

suspend fun FileSystem.safeMetadata(path: Path): okio.FileMetadata {
  if (this === ResourceFileSystem.FileSystem) {
    ResourceFileSystem.prepare(path).await()
  }
  return metadata(path)
}

suspend fun FileSystem.safeMetadataOrNull(path: Path): okio.FileMetadata? {
  if (this === ResourceFileSystem.FileSystem) {
    ResourceFileSystem.prepare(path).await()
  }
  return metadataOrNull(path)
}

suspend fun FileSystem.safeSource(path: Path): okio.Source {
  if (this === ResourceFileSystem.FileSystem) {
    ResourceFileSystem.prepare(path).await()
  }
  return source(path)
}