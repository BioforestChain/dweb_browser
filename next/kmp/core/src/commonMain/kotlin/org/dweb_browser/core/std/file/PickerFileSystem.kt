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

object PickerFileSystem {
  private val fakeFileSystem = FakeFileSystem()

  class PickerFile(val fs: FileSystem, val path: Path)

  val files = SafeHashMap<String, PickerFile>()
  inline fun <R> tryPick(path: Path, ifPicked: PickerFile.() -> R, defaultValue: () -> R) =
    files[path.toString()]?.ifPicked() ?: defaultValue()

  val FileSystem = object : ForwardingFileSystem(fakeFileSystem) {
    override fun metadataOrNull(path: Path): FileMetadata? {
      return tryPick(path, {
        fs.metadataOrNull(path)
      }) {
        super.metadataOrNull(path)
      }
    }

    override fun source(file: Path): Source {
      return tryPick(file, {
        fs.source(path)
      }) {
        super.source(file)
      }
    }

    override fun openReadOnly(file: Path): FileHandle {
      return tryPick(file, {
        fs.openReadOnly(path)
      }) {
        super.openReadOnly(file)
      }
    }


    override fun appendingSink(file: Path, mustExist: Boolean): Sink {
      throw okio.IOException("picker is readonly, not allow appendingSink")
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
      throw okio.IOException("picker is readonly, not allow sink")
    }

    override fun atomicMove(source: Path, target: Path) {
      throw okio.IOException("picker is readonly, not allow atomicMove")
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean) {
      throw okio.IOException("picker is readonly, not allow createDirectory")
    }

    override fun copy(source: Path, target: Path) {
      throw okio.IOException("picker is readonly, not allow copy")
    }

    override fun createSymlink(source: Path, target: Path) {
      throw okio.IOException("picker is readonly, not allow createSymlink")
    }

    override fun deleteRecursively(fileOrDirectory: Path, mustExist: Boolean) {
      throw okio.IOException("picker is readonly, not allow deleteRecursively")
    }

    override fun delete(path: Path, mustExist: Boolean) {
      throw okio.IOException("picker is readonly, not allow delete")
    }

    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle {
      throw okio.IOException("picker is readonly, not allow openReadWrite")
    }
  }
}