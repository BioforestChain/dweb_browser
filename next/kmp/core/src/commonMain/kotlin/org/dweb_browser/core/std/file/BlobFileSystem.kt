package org.dweb_browser.core.std.file


import okio.FileHandle
import okio.ForwardingFileSystem
import okio.Path
import okio.Sink
import org.dweb_browser.pure.io.SystemFileSystem

private const val readonlyErrorMessage = "blob is readonly, use file.std.dweb/blob/create"


val BlobReadonlyFileSystem = object : ForwardingFileSystem(SystemFileSystem) {
  override fun appendingSink(file: Path, mustExist: Boolean): Sink {
    throw okio.IOException(readonlyErrorMessage)
  }

  override fun sink(file: Path, mustCreate: Boolean): Sink {
    throw okio.IOException(readonlyErrorMessage)
  }

  override fun atomicMove(source: Path, target: Path) {
    throw okio.IOException(readonlyErrorMessage)
  }

  override fun createDirectory(dir: Path, mustCreate: Boolean) {
    throw okio.IOException(readonlyErrorMessage)
  }

  override fun copy(source: Path, target: Path) {
    throw okio.IOException(readonlyErrorMessage)
  }

  override fun createSymlink(source: Path, target: Path) {
    throw okio.IOException(readonlyErrorMessage)
  }

  override fun deleteRecursively(fileOrDirectory: Path, mustExist: Boolean) {
    throw okio.IOException(readonlyErrorMessage)
  }

  override fun delete(path: Path, mustExist: Boolean) {
    throw okio.IOException(readonlyErrorMessage)
  }

  override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle {
    throw okio.IOException(readonlyErrorMessage)
  }
}

internal val blobReadWriteVirtualFsDirectory = commonVirtualFsDirectoryFactory(
  firstSegmentFlags = "blob",
  nativeFsPath = FileNMM.getApplicationCacheDir().resolve("blob"),
  separated = false
)

val blobReadonlyVirtualFsDirectory = commonVirtualFsDirectoryFactory(
  firstSegmentFlags = "blob",
  nativeFsPath = FileNMM.getApplicationCacheDir().resolve("blob"),
  separated = false,
  fs = BlobReadonlyFileSystem
)
