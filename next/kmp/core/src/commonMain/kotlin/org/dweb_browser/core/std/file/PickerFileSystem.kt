package org.dweb_browser.core.std.file

import okio.FileHandle
import okio.FileMetadata
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Sink
import okio.Source
import okio.fakefilesystem.FakeFileSystem
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.pure.io.SystemFileSystem

object PickerFileSystem {
  private val fakeFileSystem = FakeFileSystem()
  var backFs = SystemFileSystem

  data class PickerFile(
    val fs: FileSystem,
    val path: Path,
    val canDelete: Boolean = true,
    val canWrite: Boolean = true,
  )

  val files = SafeHashMap<String, PickerFile>()

  inline fun <R> tryPick(
    path: Path,
    defaultValue: () -> R = { throw okio.IOException("no found $Path") },
    ifPicked: PickerFile.() -> R,
  ) = getPickerFile(path)?.ifPicked() ?: defaultValue()

  inline fun getPickerFile(path: Path) = files[path.toString()]

  val FileSystem = object : ForwardingFileSystem(fakeFileSystem) {
    override fun metadataOrNull(path: Path): FileMetadata? {
      return tryPick(path) {
        fs.metadataOrNull(path)
      }
    }

    override fun source(file: Path): Source {
      return tryPick(file) {
        fs.source(path)
      }
    }

    override fun openReadOnly(file: Path): FileHandle {
      return tryPick(file) {
        fs.openReadOnly(path)
      }
    }


    override fun appendingSink(file: Path, mustExist: Boolean): Sink {
      return tryPick(file) {
        if (!canWrite) {
          throw okio.IOException("picker is readonly, not allow appendingSink")
        }
        fs.appendingSink(file, mustExist)
      }
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
      return tryPick(file) {
        if (!canWrite) {
          throw okio.IOException("picker is readonly, not allow sink")
        }
        fs.sink(path, mustCreate)
      }
    }

    override fun atomicMove(source: Path, target: Path) {
      val sourceFsPath = getPickerFile(source)
      if (sourceFsPath?.canWrite == false) {
        throw okio.IOException("picker(source) is readonly, not allow move out")
      }
      val sourceFs = sourceFsPath?.fs ?: backFs
      val sourcePath = sourceFsPath?.path ?: source
      val targetFsPath = getPickerFile(source)
      if (targetFsPath?.canWrite == false) {
        throw okio.IOException("picker(target) is readonly, not allow move in")
      }
      val targetFs = targetFsPath?.fs ?: backFs
      val targetPath = targetFsPath?.path ?: source
      if (sourceFs == targetFs) {
        sourceFs.atomicMove(sourcePath, targetPath)
      } else {
        targetFs.write(targetPath, true) {
          writeAll(sourceFs.source(sourcePath))
        }
        sourceFs.delete(sourcePath)
      }
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean) {
      tryPick(dir.parent ?: throw okio.IOException("no found parent, fail to createDirectory")) {
        if (!canDelete) {
          throw okio.IOException("picker is readonly, not allow createDirectory")
        }
        fs.createDirectory(path.resolve(dir.name), mustCreate)
      }
    }

    override fun copy(source: Path, target: Path) {
      val sourceFsPath = getPickerFile(source)
      val sourceFs = sourceFsPath?.fs ?: backFs
      val sourcePath = sourceFsPath?.path ?: source
      val targetFsPath = getPickerFile(source)
      if (targetFsPath?.canWrite == false) {
        throw okio.IOException("picker(target) is readonly, not allow copy in")
      }
      val targetFs = targetFsPath?.fs ?: backFs
      val targetPath = targetFsPath?.path ?: source
      if (sourceFs == targetFs) {
        sourceFs.atomicMove(sourcePath, targetPath)
      } else {
        targetFs.write(targetPath, true) {
          writeAll(sourceFs.source(sourcePath))
        }
      }
    }

    override fun createSymlink(source: Path, target: Path) {
      val sourceFsPath = getPickerFile(source)
      val sourceFs = sourceFsPath?.fs ?: backFs
      val sourcePath = sourceFsPath?.path ?: source
      val targetFsPath = getPickerFile(source)
      if (targetFsPath?.canWrite == false) {
        throw okio.IOException("picker(target) is readonly, not allow createSymlink")
      }
      val targetFs = targetFsPath?.fs ?: backFs
      val targetPath = targetFsPath?.path ?: source
      if (sourceFs == targetFs) {
        sourceFs.createSymlink(sourcePath, targetPath)
      } else {
        throw okio.IOException("picker not allow createSymlink between different file system")
      }
    }

    override fun deleteRecursively(fileOrDirectory: Path, mustExist: Boolean) {
      tryPick(fileOrDirectory) {
        if (!canDelete) {
          throw okio.IOException("picker is readonly, not allow deleteRecursively")
        }
        fs.deleteRecursively(path, mustExist)
      }
    }

    override fun delete(path: Path, mustExist: Boolean) {
      tryPick(path) {
        if (!canDelete) {
          throw okio.IOException("picker is readonly, not allow delete")
        }
        fs.deleteRecursively(path, mustExist)
      }
    }

    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle {
      return tryPick(file) {
        if (!canDelete) {
          throw okio.IOException("picker is readonly, not allow openReadWrite")
        }
        fs.openReadWrite(file, mustCreate, mustExist)
      }
    }
  }
}