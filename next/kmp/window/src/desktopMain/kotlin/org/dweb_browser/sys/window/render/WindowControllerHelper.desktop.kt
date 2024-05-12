package org.dweb_browser.sys.window.render

import androidx.compose.ui.graphics.toAwtImage
import dweb_browser_kmp.window.generated.resources.Res
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.pure.image.OffscreenWebCanvas
import org.dweb_browser.pure.image.compose.WebImageLoader
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

actual val WindowController.canOverlayNavigationBar: Boolean
  get() = false

/**
 * Windows 操作系统背景不透明，所以始终为0
 */
actual fun getWindowControllerBorderRounded(isMaximize: Boolean) =
  WindowPadding.CornerRadius.from(0) //始终设置为0，桌面端设置16的圆角不好看

// if (isMaximize || PureViewController.isWindows) 0 else 16


@OptIn(ExperimentalResourceApi::class)
suspend fun MicroModule.Runtime.loadAwtIconImage(): BufferedImage =
  icons.toStrict().pickLargest()?.src?.let { url ->
    WebImageLoader.defaultInstance.load(
      OffscreenWebCanvas.defaultInstance, url, 128, 128, imageFetchHook
    ).firstOrNull {
      it.isSuccess
    }?.success?.toAwtImage()
  } ?: withContext(ioAsyncExceptionHandler) {
    ImageIO.read(Res.readBytes("files/sys-icons/notification_default_icon.png").inputStream())
  }

val MMR_awtIconImage_WM = WeakHashMap<MicroModule.Runtime, Deferred<BufferedImage>>()
val MicroModule.Runtime.awtIconImage
  get() = MMR_awtIconImage_WM.getOrPut(this) {
    getRuntimeScope().async {
      loadAwtIconImage()
    }
  }
