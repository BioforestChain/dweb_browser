package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter

data class ImageLoadResult(
  val success: ImageBitmap? = null,
  val error: Throwable? = null,
  val busy: String? = null,
) {
  companion object {

    internal fun success(success: ImageBitmap) = ImageLoadResult(success = success)
    internal fun error(error: Throwable?) = ImageLoadResult(error = error)

    internal val Setup = ImageLoadResult(busy = "setup...")
    internal val Loading = ImageLoadResult(busy = "loading and rendering...")
  }

  val isSuccess get() = success != null
  val isError get() = error != null
  val isBusy get() = busy != null
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

  @Composable
  fun painter() = remember(success) { success?.let { BitmapPainter(it) } }
}