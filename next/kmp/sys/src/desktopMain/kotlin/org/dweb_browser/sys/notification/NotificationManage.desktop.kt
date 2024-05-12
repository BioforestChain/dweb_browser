package org.dweb_browser.sys.notification

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.window.render.awtIconImage
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.awt.SystemTray
import java.awt.TrayIcon

actual class NotificationManager actual constructor() {
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

  val isSupport = SystemTray.isSupported()
//  private val tray = SystemTray.getSystemTray();

  @OptIn(ExperimentalResourceApi::class)
  actual suspend fun createNotification(
    microModule: MicroModule.Runtime,
    message: NotificationWebItem,
  ) {
    if (!isSupport) {
      return
    }

    // 创建一个托盘图标
    val image = microModule.awtIconImage.await()

    val trayIcon = TrayIcon(image, microModule.name)

    // 让托盘图标自动调整到适合系统托盘的大小
    trayIcon.isImageAutoSize = true
    trayIcon.displayMessage(
      message.actions.firstOrNull()?.title ?: microModule.mmid,
      message.body,
      TrayIcon.MessageType.INFO
    )
  }

}