package org.dweb_browser.sys.notification

import androidx.compose.ui.graphics.toAwtImage
import dweb_browser_kmp.sys.generated.resources.Res
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.pure.image.OffscreenWebCanvas
import org.dweb_browser.pure.image.compose.WebImageLoader
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict
import org.dweb_browser.sys.window.render.imageFetchHook
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.awt.SystemTray
import java.awt.TrayIcon
import javax.imageio.ImageIO

actual class NotificationManager actual constructor() {
  val isSupport = SystemTray.isSupported()
//  private val tray = SystemTray.getSystemTray();

  @OptIn(ExperimentalResourceApi::class)
  actual suspend fun createNotification(microModule: MicroModule, message: NotificationMsgItem) {
    if (!isSupport) {
      return
    }

    // 创建一个托盘图标
    val image = microModule.icons.toStrict().pickLargest()?.src?.let { url ->
      WebImageLoader.defaultInstance.load(
        OffscreenWebCanvas.defaultInstance, url, 128, 128, microModule.imageFetchHook
      ).firstOrNull {
        it.isSuccess
      }?.success?.toAwtImage()
    } ?:
    // TODO 使用 withContext(ioAsyncExceptionHandler) {
    withContext(ioAsyncExceptionHandler) {
      ImageIO.read(Res.readBytes("drawable/notification_default_icon.png").inputStream())
    }

    val trayIcon = TrayIcon(image, microModule.name);

    // 让托盘图标自动调整到适合系统托盘的大小
    trayIcon.isImageAutoSize = true;
    trayIcon.displayMessage(message.title, message.msg_content, TrayIcon.MessageType.INFO)
  }

}