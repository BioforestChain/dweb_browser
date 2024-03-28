package org.dweb_browser.sys.notification

import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

actual class NotificationManager {
  init {
    SystemPermissionAdapterManager.append {
      when (task.name) {
        SystemPermissionName.Notification -> {
          AuthorizationStatus.GRANTED
        }

        else -> null
      }
    }
  }

  actual suspend fun createNotification(microModule: MicroModule, message: NotificationWebItem) {
    TODO("No yet implement createNotification")
  }
}