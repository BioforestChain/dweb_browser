package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asDesktopBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skiko.toBufferedImage
import org.jetbrains.skiko.toImage
import java.util.Base64

interface PureImageLoader {
  @Composable
  fun Load(
    url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook?
  ): ImageLoadResult

  companion object {
    @Composable
    fun SmartLoad(
      url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook? = null
    ): ImageLoadResult {
      var fixUrl = url
      if (fixUrl.startsWith("data://localhost/")) {
        fixUrl = fixUrl.replace("data://localhost/", "data:")
      }
      val result = LocalCoilImageLoader.current.Load(fixUrl, maxWidth, maxHeight, hook)
      if (result.isError) {
        return LocalWebImageLoader.current.Load(fixUrl, maxWidth, maxHeight, hook)
      }
      return result
    }

    @Composable
    fun WebLoad(
      url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook? = null
    ): ImageLoadResult {
      var fixUrl = url
      if (fixUrl.startsWith("data://localhost/")) {
        fixUrl = fixUrl.replace("data://localhost/", "data:")
      }

      return LocalWebImageLoader.current.Load(fixUrl, maxWidth, maxHeight, hook)
    }
  }
}