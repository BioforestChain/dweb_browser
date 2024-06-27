package org.dweb_browser.core.std.file

import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import org.dweb_browser.platform.desktop.os.dataDir
import org.dweb_browser.platform.desktop.os.rootDir
import org.dweb_browser.platform.desktop.os.blobDir


/**
 * 获取应用内部目录
 */
actual fun FileNMM.Companion.getApplicationRootDir() = rootDir.toOkioPath()

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
  "cache", System.getProperty("java.io.tmpdir").toPath(true).resolve("dweb-browser")
)

/**
 * 外部下载文件夹，这里的空间不会被回收
 */
actual fun FileNMM.getExternalDownloadVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "download", System.getProperty("user.home").toPath(true).resolve("Downloads/dweb-browser")
)

actual fun FileNMM.getBlobVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "blob", blobDir.toOkioPath(), false
)