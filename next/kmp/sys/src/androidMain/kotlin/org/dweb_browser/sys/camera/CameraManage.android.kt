package org.dweb_browser.sys.camera

import android.Manifest
import org.dweb_browser.helper.WARNING
import org.dweb_browser.sys.permission.AndroidPermissionTask
import org.dweb_browser.sys.permission.PermissionActivity
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

actual class CameraManage actual constructor(){
  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.CAMERA) {
        PermissionActivity.launchAndroidSystemPermissionRequester(
          microModule,
          AndroidPermissionTask(Manifest.permission.CAMERA, task.title, task.description)
        ).values.firstOrNull()
      } else null
    }
  }

  actual fun cameraPermission(): Boolean {
    WARNING("Not yet implemented cameraPermission")
    return true
  }
}