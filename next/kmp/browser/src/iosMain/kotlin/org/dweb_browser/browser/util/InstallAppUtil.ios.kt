package org.dweb_browser.browser.util

import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.WARNING
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

actual class InstallAppUtil {
  init {
    SystemPermissionAdapterManager.append {
      when (task.name) {
        SystemPermissionName.InstallSystemApp -> {
          WARNING("Not yet implement InstallSystemApp Permission")
          AuthorizationStatus.GRANTED
        }

        else -> null
      }
    }
  }

  /**
   * 获取当前版本，存储的版本，以及在线加载最新版本
   */
  actual suspend fun loadNewVersion(): NewVersionItem? {
    WARNING("Not yet implement loadNewVersion")
    return null
  }

  actual fun openSystemInstallSetting() { // 打开系统的授权安装界面
    WARNING("Not yet implement openSystemInstallSetting")
  }

  actual fun installApp(realPath: String): Boolean { // 安装应用
    WARNING("Not yet implement installApp")
    return false
  }

  actual fun openOrShareFile(realPath: String) {
    WARNING("Not yet implement openOrShareFile => $realPath")
  }
}