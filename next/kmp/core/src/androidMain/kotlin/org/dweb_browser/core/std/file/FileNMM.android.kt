package org.dweb_browser.core.std.file

import okio.Path.Companion.toPath
import org.dweb_browser.core.module.getAppContext

actual fun FileNMM.Companion.getApplicationRootDir() = getAppContext().dataDir.absolutePath.toPath()

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
  "cache", getAppContext().cacheDir.absolutePath.toPath()
)
