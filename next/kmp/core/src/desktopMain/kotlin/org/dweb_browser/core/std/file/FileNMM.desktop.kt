package org.dweb_browser.core.std.file

import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import org.dweb_browser.platform.desktop.os.dataDir
import org.dweb_browser.platform.desktop.os.rootDir


/**
 * 获取应用内部目录
 */
actual fun FileNMM.Companion.getApplicationRootDir() = rootDir.toOkioPath(true)
actual fun FileNMM.Companion.getApplicationCacheDir() =
  System.getProperty("java.io.tmpdir").toPath(true).resolve("dweb-browser")

/**
 * 持久化数据
 */
actual fun FileNMM.getDataVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "data", dataDir.toOkioPath()
)

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
  "download", System.getProperty("user.home").toPath(true).resolve("Downloads/dweb-browser")
)
