package org.dweb_browser.core.std.file

import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask


actual fun FileNMM.Companion.getApplicationRootDir() = NSSearchPathForDirectoriesInDomains(
  NSApplicationDirectory, NSUserDomainMask, true
).first().toString().toPath()

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
  "cache", NSSearchPathForDirectoriesInDomains(
    NSCachesDirectory, NSUserDomainMask, true
  ).first().toString()
)
