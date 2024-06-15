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
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
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

suspend fun MicroModule.Runtime.loadSourceToImageBitmap(src: String, width: Int, height: Int) =
  WebImageLoader.defaultInstance.load(
    OffscreenWebCanvas.defaultInstance, src, width, height, imageFetchHook
  ).firstOrNull {
    it.isSuccess
  }?.success

@OptIn(ExperimentalResourceApi::class)
suspend fun MicroModule.Runtime.loadIconAsAwtImage(): BufferedImage =
  icons.toStrict().pickLargest()?.src?.let { url ->
    loadSourceToImageBitmap(url, 256, 256)?.toAwtImage()
  } ?: withContext(ioAsyncExceptionHandler) {
    ImageIO.read(Res.readBytes("files/sys-icons/notification_default_icon.png").inputStream())
  }

val MMR_awtIconImage_WM = WeakHashMap<MicroModule.Runtime, Deferred<BufferedImage>>()
val MicroModule.Runtime.awtIconImage
  get() = MMR_awtIconImage_WM.getOrPut(this) {
    getRuntimeScope().async {
      loadIconAsAwtImage()
    }
  }


fun BufferedImage.rounded(roundX: Float, roundY: Float = roundX): BufferedImage {
  val input = this
  val output = BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_ARGB)

  val ctx = output.createGraphics()
  ctx.composite = AlphaComposite.Src
  ctx.setRenderingHint(
    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON
  )

  // Draw the rounded filled rectangle
  ctx.color = Color.WHITE
  ctx.fill(
    RoundRectangle2D.Float(
      0f, 0f, input.width.toFloat(), input.height.toFloat(), roundX, roundY
    )
  )

  // Set the composite to only paint inside the rectangle
  ctx.composite = AlphaComposite.SrcIn
  ctx.drawImage(input, 0, 0, null)
  ctx.dispose()
  return output
}

val MMR_awtIconRoundedImage_WM = WeakHashMap<MicroModule.Runtime, Deferred<BufferedImage>>()

val MicroModule.Runtime.awtIconRoundedImage: Deferred<BufferedImage>
  get() = MMR_awtIconRoundedImage_WM.getOrPut(this) {
    getRuntimeScope().async {
      awtIconImage.await().let { it.rounded(it.width * 0.38f, it.height * 0.38f) }
    }
  }