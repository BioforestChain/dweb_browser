package org.dweb_browser.browser.web

import kotlinx.coroutines.withContext
import org.dweb_browser.browser.web.model.DwebLinkSearchItem
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.ioAsyncExceptionHandler
import java.awt.Desktop
import java.io.File

actual fun getImageResourceRootPath(): String {
  WARNING("Not yet implemented getImageResourceRootPath")
  return ""
}

actual suspend fun deepLinkDoSearch(dwebLinkSearchItem: DwebLinkSearchItem) {
  // 不需要实现，这个目前是专门给 Ios 使用
}

actual suspend fun openFileByPath(realPath: String, justInstall: Boolean): Boolean {
  if (Desktop.isDesktopSupported()) {
    val desktop = Desktop.getDesktop()
    if (desktop.isSupported(Desktop.Action.OPEN)) {
      try {
        withContext(ioAsyncExceptionHandler) {
          desktop.open(File(realPath))
        }
        return true
      } catch (e: Exception) {
        println("openFileByPath fail => ${e.message}")
      }
    } else {
      println("openFileByPath not supported Open")
    }
  } else {
    println("openFileByPath Desktop not supported this environment")
  }
  return false
}