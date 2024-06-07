package org.dweb_browser.browser.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.util.InstallApkUtil
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.DwebLinkSearchItem
import org.dweb_browser.browser.web.ui.BrowserViewModalRender
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import java.io.File

actual fun getImageResourceRootPath(): String {
  return getAppContextUnsafe().filesDir.absolutePath + "/icons"
}

@Composable
actual fun CommonBrowserView(
  viewModel: BrowserViewModel, modifier: Modifier, windowRenderScope: WindowContentRenderScope,
) {
  BrowserViewModalRender(viewModel, modifier, windowRenderScope)
}

actual suspend fun deepLinkDoSearch(dwebLinkSearchItem: DwebLinkSearchItem) {
  // 不需要实现，这个目前是专门给 Ios 使用
}

actual suspend fun openFileByPath(realPath: String, justInstall: Boolean): Boolean {
  val fileName = realPath.substringAfterLast(File.separator)
  val suffix = fileName.substringAfterLast(".") // 获取后缀名
  val context = getAppContextUnsafe()
  return if ("apk" == suffix) { // 表示是安卓安装程序，那么就进行安装权限判断
    if (InstallApkUtil.enableInstallApp(context)) {
      InstallApkUtil.installApp(context = context, realPath = realPath)
      true
    } else {
      InstallApkUtil.openSystemInstallSetting(context)
      false
    }
  } else {
    if (!justInstall) InstallApkUtil.openFile(realPath) else false
  }
}