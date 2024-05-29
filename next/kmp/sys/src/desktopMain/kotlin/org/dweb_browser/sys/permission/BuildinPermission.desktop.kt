package org.dweb_browser.sys.permission

import org.dweb_browser.core.std.permission.AuthorizationStatus

actual object BuildinPermission {
  actual fun start() {
    SystemPermissionAdapterManager.append {
      when (task.name) {
        SystemPermissionName.CAMERA, SystemPermissionName.MICROPHONE, SystemPermissionName.LOCATION, SystemPermissionName.STORAGE, SystemPermissionName.Notification -> AuthorizationStatus.GRANTED
        else -> null
      }
    }
  }
}

