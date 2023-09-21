package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import org.dweb_browser.helper.platform.OffscreenWebCanvas
import org.dweb_browser.helper.platform.offscreenwebcanvas.FetchHook
import org.dweb_browser.helper.platform.offscreenwebcanvas.WebCanvasContextSession.Companion.buildTask
import org.dweb_browser.helper.platform.offscreenwebcanvas.waitReady
import org.dweb_browser.helper.platform.setHook


@Composable
fun rememberImageLoader(): ImageLoader {
  return remember {
    ImageLoader()
  }
}

@Composable
internal expect fun rememberOffscreenWebCanvas(): OffscreenWebCanvas


class ImageLoader {
  @Composable
  fun load(
    url: String,
    maxWidth: Dp,
    maxHeight: Dp,
    hook: FetchHook? = null
  ): ImageLoadResult {
    val density = LocalDensity.current.density
    val containerWidth = (maxWidth.value * density).toInt()
    val containerHeight = (maxHeight.value * density).toInt()
    return load(url, containerWidth, containerHeight, hook)
  }

  @Composable
  fun load(
    url: String, containerWidth: Int, containerHeight: Int, hook: (FetchHook)? = null
  ): ImageLoadResult {
    val webCanvas = rememberOffscreenWebCanvas();
    val imageBitmap by produceState(ImageLoadResult.Setup) {
      val dispose = if (hook != null) {
        webCanvas.setHook(url, hook)
      } else null
      value = try {
        webCanvas.waitReady()
        value = ImageLoadResult.Rendering;
        val imageBitmap = webCanvas.buildTask {
          renderImage(url, containerWidth, containerHeight)
          toImageBitmap()
        }
        ImageLoadResult.success(imageBitmap)
      } catch (e: Throwable) {
        ImageLoadResult.error(e)
      } finally {
        dispose?.invoke()
      }
    }
    return imageBitmap
  }
}

class ImageLoadResult(
  val success: ImageBitmap? = null,
  val error: Throwable? = null,
  val busy: String? = null,
) {
  companion object {

    internal fun success(success: ImageBitmap) = ImageLoadResult(success = success)
    internal fun error(error: Throwable?) = ImageLoadResult(error = error)

    internal val Setup = ImageLoadResult(busy = "setup...")
    internal val Rendering = ImageLoadResult(busy = "loading and rendering...")
  }

  val isSuccess get() = success != null
  inline fun with(
    onBusy: (String) -> Unit = {},
    onError: (Throwable) -> Unit = {},
    onSuccess: (ImageBitmap) -> Unit = {},
  ) {
    if (success != null) {
      onSuccess(success)
    } else if (error != null) {
      onError(error)
    } else if (busy != null) {
      onBusy(busy)
    }
  }
}