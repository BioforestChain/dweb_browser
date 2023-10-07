package org.dweb_browser.microservice.std.file

import okio.Path.Companion.toPath
import org.dweb_browser.microservice.core.AndroidNativeMicroModule


/**
 * 持久化数据
 */
actual fun FileNMM.getDataVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "data", AndroidNativeMicroModule.appContext.dataDir.absolutePath.toPath()
)

/**
 * 缓存文件夹，这里的空间会被按需回收
 */
actual fun FileNMM.getCacheVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "cache", AndroidNativeMicroModule.appContext.cacheDir.absolutePath.toPath()
)
