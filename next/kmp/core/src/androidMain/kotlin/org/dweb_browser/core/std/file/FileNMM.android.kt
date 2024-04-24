package org.dweb_browser.core.std.file

import android.os.Environment
import okio.Path.Companion.toPath
import org.dweb_browser.helper.getAppContextUnsafe

actual fun FileNMM.Companion.getApplicationRootDir() = getAppContextUnsafe().dataDir.absolutePath.toPath()

/**
 * 持久化数据
 */
actual fun FileNMM.getDataVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "data", FileNMM.Companion.getApplicationRootDir().resolve("data")
)

/**
 * 缓存文件夹，这里的空间会被按需回收
 */
actual fun FileNMM.getCacheVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "cache", getAppContextUnsafe().cacheDir.absolutePath.toPath()
)

/**
 * 外部下载文件夹，这里的空间不会被回收
 */
actual fun FileNMM.getExternalDownloadVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  firstSegmentFlags = "download",
  nativeFsPath = getAppContextUnsafe().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath.toPath()
)