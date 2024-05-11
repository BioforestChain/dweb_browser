package org.dweb_browser.core.std.file

import dweb_browser_kmp.core.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okio.FileHandle
import okio.FileMetadata
import okio.ForwardingFileSystem
import okio.Path
import okio.Sink
import okio.Source
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
object ResourceFileSystem {
  private val fakeFileSystem = FakeFileSystem()
  private val fsScope = CoroutineScope(ioAsyncExceptionHandler)
  private val sinkMap = SafeHashMap<String, Deferred<Boolean>>()
  fun prepare(path: Path) = sinkMap.getOrPut(path.toString()) {
    fsScope.async(start = CoroutineStart.UNDISPATCHED) {
      try {
        Res.getUri("files$path")
        path.parent?.also {
          fakeFileSystem.createDirectories(it, false)
        }
        val sink = fakeFileSystem.sink(path, true)

        val content = Res.readBytes("files$path")
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
    runBlocking { await() }
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