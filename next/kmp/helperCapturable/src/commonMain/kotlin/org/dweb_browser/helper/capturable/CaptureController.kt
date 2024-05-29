package org.dweb_browser.helper.capturable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Controller for capturing [Composable] content.
 * @see dev.shreyaspatil.capturable.Capturable for implementation details.
 */
class CaptureController {

  /**
   * Medium for providing capture requests
   */
  @Suppress("ktlint")
  private val _captureRequests = MutableSharedFlow<CaptureRequest>(extraBufferCapacity = 1)
  internal val captureRequests = _captureRequests.asSharedFlow()


  /**
   * with specified [config] and returns an [ImageBitmap] asynchronously.
   *
   * This method is safe to be called from the "main" thread directly.
   *
   * Make sure to call this method as a part of callback function and not as a part of the
   * [Composable] function itself.
   *
   * @param config ImageBitmap config
   */
  private var _deferredImageBitmap = CompletableDeferred<ImageBitmap>().apply {
    completeExceptionally(Throwable("no capture"))
  }

  fun captureAsync(config: CaptureConfig = CaptureConfig.default) =
    CompletableDeferred<ImageBitmap>().let {
      if (_captureRequests.tryEmit(CaptureRequest(imageBitmapDeferred = it, config = config))) {
        _deferredImageBitmap = it
        it.invokeOnCompletion {}
        it
      } else {
        _deferredImageBitmap
      }
    }


  /**
   * Holds information of capture request
   */
  internal class CaptureRequest(
    val imageBitmapDeferred: CompletableDeferred<ImageBitmap>, val config: CaptureConfig
  )

  class CaptureConfig(internal val source: MutableMap<String, Any>) {
    companion object {
      val default = CaptureConfig(mutableMapOf())
    }
  }
}

/**
 * Creates [CaptureController] and remembers it.
 */
@Composable
fun rememberCaptureController(): CaptureController {
  return remember { CaptureController() }
}