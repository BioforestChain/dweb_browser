package org.dweb_browser.platform.desktop.os

import java.nio.file.Paths

val rootDir by lazy {
  when (OsType.current) {
    OsType.MacOS -> Paths.get(System.getProperty("user.home"))
      .resolve("Library/Application Support/dweb-browser")

    // TODO: 修改为自己启动的应用内目录
    OsType.Windows -> System.getenv("APPDATA").let { appData ->
      if (appData.isBlank()) {
        Paths.get(System.getProperty("user.home")).resolve(".dweb-browser")
      } else {
        Paths.get(appData).resolve("Local/dweb-browser")
      }
    }

    else -> Paths.get(System.getProperty("user.home")).resolve(".dweb-browser")
  }
}

val dataDir by lazy {
  rootDir.resolve("data")
}