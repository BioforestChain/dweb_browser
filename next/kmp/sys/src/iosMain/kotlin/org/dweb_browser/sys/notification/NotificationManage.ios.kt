package org.dweb_browser.sys.notification

import org.dweb_browser.core.module.MicroModule

actual class NotificationManager {
  actual suspend fun createNotification(
    microModule: MicroModule.Runtime,
    message: NotificationWebItem
  ) {
    TODO("No yet implement createNotification")
  }
}