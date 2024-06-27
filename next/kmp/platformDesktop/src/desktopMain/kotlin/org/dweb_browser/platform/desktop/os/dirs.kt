package org.dweb_browser.platform.desktop.os

import java.nio.file.Paths


val rootDir = Paths.get("")

val dataDir by lazy {
  when (OsType.current) {
    OsType.MacOS -> Paths.get(System.getProperty("user.home"))
      .resolve("Library/Application Support/dweb-browser/data")

    // TODO: 修改为自己启动的应用内目录
    OsType.Windows -> System.getenv("APPDATA").let { appData ->
      if (appData.isBlank()) {
        rootDir.resolve("data")
      } else {
        Paths.get(appData).resolve("Local/dweb-browser/data")
      }
    }

    else -> rootDir.resolve("data")
  }
}

val blobDir by lazy {
  when (OsType.current) {
    OsType.MacOS -> Paths.get(System.getProperty("user.home"))
      .resolve("Library/Application Support/dweb-browser/blob")

    // TODO: 修改为自己启动的应用内目录
    OsType.Windows -> System.getenv("APPDATA").let { appData ->
      if (appData.isBlank()) {
        rootDir.resolve("blob")
      } else {
        Paths.get(appData).resolve("Local/dweb-browser/blob")
      }
    }

    else -> rootDir.resolve("blob")
  }
}