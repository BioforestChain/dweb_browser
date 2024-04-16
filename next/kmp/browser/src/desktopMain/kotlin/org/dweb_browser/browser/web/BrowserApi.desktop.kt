package org.dweb_browser.browser.web

import org.dweb_browser.browser.web.model.DwebLinkSearchItem
import org.dweb_browser.helper.WARNING

actual fun getImageResourceRootPath(): String {
  WARNING("Not yet implemented getImageResourceRootPath")
  return ""
}

actual suspend fun deepLinkDoSearch(dwebLinkSearchItem: DwebLinkSearchItem) {
  // 不需要实现，这个目前是专门给 Ios 使用
}

actual suspend fun openFileByPath(realPath: String, justInstall: Boolean): Boolean {
  WARNING("Not yet implement openFileByPath")
  return false
}