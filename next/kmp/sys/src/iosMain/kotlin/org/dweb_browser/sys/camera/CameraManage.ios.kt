package org.dweb_browser.sys.camera

import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.WARNING
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

actual class CameraManage actual constructor() {
  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.CAMERA) {
        WARNING("Not yet implemented CameraManage")
        AuthorizationStatus.GRANTED
      } else null
    }
  }

  actual fun cameraPermission(): Boolean {
    WARNING("Not yet implemented cameraPermission")
    return true
  }
}