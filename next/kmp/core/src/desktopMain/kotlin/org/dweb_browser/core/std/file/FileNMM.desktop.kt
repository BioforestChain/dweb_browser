package org.dweb_browser.core.std.file

import okio.Path.Companion.toPath
import org.dweb_browser.platform.desktop.os.OsType


/**
 * 获取应用内部目录
 */
actual fun FileNMM.Companion.getApplicationRootDir() =
  java.nio.file.Paths.get("").toAbsolutePath().toString().toPath()

/**
 * 持久化数据
 */
actual fun FileNMM.getDataVirtualFsDirectory() = commonVirtualFsDirectoryFactory(
  "data", when (OsType.current) {
    OsType.MacOS -> System.getProperty("user.home").toPath(true)
      .resolve("Library/Application Support/dweb-browser/data")

    // TODO: 修改为自己启动的应用内目录
    OsType.Windows -> System.getenv("APPDATA").let { appData ->
      if (appData.isBlank()) {
        FileNMM.Companion.getApplicationRootDir().resolve("data")
      } else {
        appData.toPath(true).resolve("Local/dweb-browser/data")
      }
    }

    else -> FileNMM.getApplicationRootDir().resolve("data")
  }
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