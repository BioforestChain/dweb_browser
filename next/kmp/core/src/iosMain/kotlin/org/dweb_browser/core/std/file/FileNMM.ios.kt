package org.dweb_browser.core.std.file

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.pure.io.SystemFileSystem
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask


actual fun FileNMM.Companion.getApplicationRootDir() = NSSearchPathForDirectoriesInDomains(
  NSDocumentDirectory, NSUserDomainMask, true
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

/**
 * 外部下载文件夹，这里的空间不会被回收??
 */
actual fun FileNMM.getExternalDownloadVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "download", NSSearchPathForDirectoriesInDomains(
    NSCachesDirectory, NSUserDomainMask, true
  ).first().toString()
)

actual fun FileNMM.getBlobVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "blob", FileNMM.Companion.getApplicationRootDir().resolve("blob"), false
)