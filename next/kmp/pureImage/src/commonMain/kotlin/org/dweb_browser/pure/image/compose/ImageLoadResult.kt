package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import coil3.Image
import coil3.request.ImageRequest
import coil3.request.SuccessResult

data class ImageLoadResult(
  val success: ImageBitmap? = null,
  val error: Throwable? = null,
  val busy: String? = null,
  val coilImageRequest: ImageRequest? = null,
  val coilSuccessResult: SuccessResult? = null,
) {
  companion object {
    internal fun success(
      success: ImageBitmap,
      coilImageRequest: ImageRequest? = null,
      coilImageResult: SuccessResult? = null
    ) = ImageLoadResult(
      success = success,
      coilImageRequest = coilImageRequest,
      coilSuccessResult = coilImageResult
    )

    internal fun error(error: Throwable?) = ImageLoadResult(error = error)

    internal val Setup = ImageLoadResult(busy = "setup...")
    internal val Loading = ImageLoadResult(busy = "loading and rendering...")
  }

  val isSuccess get() = success != null
  val isError get() = error != null
  val isBusy get() = busy != null
  inline fun with(
    onBusy: ImageLoadResult.(String) -> Unit = {},
    onError: ImageLoadResult.(Throwable) -> Unit = {},
    onSuccess: ImageLoadResult.(ImageBitmap) -> Unit = {},
  ) {
    if (success != null) {
      onSuccess(this, success)
    } else if (error != null) {
      onError(this, error)
    } else if (busy != null) {
      onBusy(this, busy)
    }
  }

  @Composable
  fun painter() = remember(success) { success?.let { BitmapPainter(it) } }
}

fun Image.Render() {
}