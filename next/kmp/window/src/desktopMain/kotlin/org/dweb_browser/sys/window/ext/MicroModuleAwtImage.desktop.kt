package org.dweb_browser.sys.window.ext

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict
import org.dweb_browser.sys.window.render.appIcon
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Image
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun MicroModule.Runtime.loadSourceToImageBitmap(src: String, width: Int, height: Int) =
  suspendCoroutine { con ->
    val key = "got image bitmap: ${randomUUID()} $src"
    val render: @Composable ApplicationScope.() -> Unit = {
      val result = PureImageLoader.SmartLoad(src, width.dp, height.dp, null, blobFetchHook)
      when {
        result.isBusy -> {}
        else -> {
          con.resume(result.success)
          PureViewController.contents -= key
        }
      }
    }
    PureViewController.contents += key to render
  }

suspend fun MicroModule.Runtime.loadIconAsAwtImage(): Image =
  icons.toStrict().pickLargest()?.src?.let { url ->
    loadSourceToImageBitmap(url, 64, 64)?.toAwtImage()
  } ?: appIcon.await().awtImage

val MMR_awtIconImage_WM = WeakHashMap<MicroModule.Runtime, Deferred<Image>>()
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
      val iconImage = awtIconImage.await()
      BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB).apply {
        graphics.drawImage(iconImage, 0, 0, null)
        graphics.dispose()
      }.rounded(64 * 0.38f, 64 * 0.38f)
    }
  }