package org.dweb_browser.core.std.file

import android.os.Environment
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.pure.io.SystemFileSystem

actual fun FileNMM.Companion.getApplicationRootDir() =
  getAppContextUnsafe().dataDir.absolutePath.toPath()

actual fun FileNMM.Companion.getApplicationCacheDir() =
  getAppContextUnsafe().cacheDir.absolutePath.toPath()

/**
 * 持久化数据
 */
actual fun FileNMM.getDataVirtualFsDirectory() = object : VirtualFsDirectory {
  override fun isMatch(firstSegment: String) = firstSegment == "data"
  override val fs: FileSystem = SystemFileSystem
  val rootDir = FileNMM.getApplicationRootDir()
  val basePath = rootDir.resolve("data")
  private val basePathStr = basePath.toString()
  override fun resolveTo(remote: IMicroModuleManifest, virtualFullPath: Path): Path {
    return when {
      /// android 的路径是 /data 开头，所以要判断一下，避免误会
      virtualFullPath.toString().startsWith(basePathStr) -> {
        virtualFullPath
      }

      else -> basePath.resolve(remote.mmid) + (virtualFullPath - "/data")
    }
  }
}

/**
 * 缓存文件夹，这里的空间会被按需回收
 */
actual fun FileNMM.getCacheVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "cache", FileNMM.Companion.getApplicationCacheDir()
)

/**
 * 外部下载文件夹，这里的空间不会被回收
 */
actual fun FileNMM.getExternalDownloadVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  firstSegmentFlags = "download",
  nativeFsPath = getAppContextUnsafe().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath.toPath()
)
