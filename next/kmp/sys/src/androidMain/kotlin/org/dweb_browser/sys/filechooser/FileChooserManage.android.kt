package org.dweb_browser.sys.filechooser

import android.Manifest
import android.os.Build
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.sys.permission.AndroidPermissionTask
import org.dweb_browser.sys.permission.PermissionActivity
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

actual class FileChooserManage {
  init {
    SystemPermissionAdapterManager.append {
      when (task.name) {
        SystemPermissionName.FILE_CHOOSER -> AuthorizationStatus.GRANTED
        SystemPermissionName.STORAGE -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) { // Android 6.0 (API等级23) 到 Android 10 (API等级29):WRITE_EXTERNAL_STORAGE和READ_EXTERNAL_STORAGE是危险权限
            PermissionActivity.launchAndroidSystemPermissionRequester(
              microModule, AndroidPermissionTask(
                listOf(
                  Manifest.permission.WRITE_EXTERNAL_STORAGE,
                  Manifest.permission.READ_EXTERNAL_STORAGE
                ), task.title, task.description
              )
            ).values.firstOrNull()
          } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S_V2) { // Android 11 (API等级30) 及以上: WRITE_EXTERNAL_STORAGE标记为已废弃。READ_EXTERNAL_STORAGE仍是一个危险权限
            PermissionActivity.launchAndroidSystemPermissionRequester(
              microModule, AndroidPermissionTask(
                listOf(
                  Manifest.permission.READ_EXTERNAL_STORAGE
                ), task.title, task.description
              )
            ).values.firstOrNull()
          } else {
            AuthorizationStatus.GRANTED
          }
        }

        else -> null
      }
    }
  }

  actual suspend fun openFileChooser(
    microModule: MicroModule, accept: String, multiple: Boolean, limit: Int
  ): List<String> {
    return FileChooserActivity.launchAndroidFileChooser(microModule, accept, multiple, limit)
  }
}
