package org.dweb_browser.core.std.file

import platform.Foundation.NSApplicationDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSFileManager



/**
 * 持久化数据
 */
actual fun FileNMM.getDataVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "data", NSSearchPathForDirectoriesInDomains(
    NSApplicationDirectory, NSUserDomainMask, true
  ).first().toString()
)

/**
 * 缓存文件夹，这里的空间会被按需回收
 */
actual fun FileNMM.getCacheVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "cache", NSSearchPathForDirectoriesInDomains(
    NSCachesDirectory, NSUserDomainMask, true
  ).first().toString()
)

actual fun FileNMM.unCompress(compressFile: String, unCompressDirectory: String) {
  // TODO()
}
