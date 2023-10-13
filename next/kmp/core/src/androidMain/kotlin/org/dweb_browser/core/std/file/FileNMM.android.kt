package org.dweb_browser.core.std.file

import okio.Path.Companion.toPath
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.ZipUtil

/**
 * 持久化数据
 */
actual fun FileNMM.getDataVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "data", "${getAppContext().dataDir.absolutePath}/data".toPath()
)

/**
 * 缓存文件夹，这里的空间会被按需回收
 */
actual fun FileNMM.getCacheVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "cache", getAppContext().cacheDir.absolutePath.toPath()
)

actual fun FileNMM.unCompress(compressFile: String, unCompressDirectory: String) {
  ZipUtil.ergodicDecompress(
    compressFile, unCompressDirectory
  )
}
