package info.bagen.rust.plaoc.ui.view

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Composable
fun Captureable(
  controller: CaptureController,
  modifier: Modifier = Modifier,
  onCaptured: (ImageBitmap?, Throwable?) -> Unit,
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  AndroidView(
    factory = { ComposeView(it).applyCapture(controller, onCaptured, content, context) },
    modifier = modifier
  )
}

/**
 * Sets the [content] in [ComposeView] and handles the capture of a [content].
 */
private inline fun ComposeView.applyCapture(
  controller: CaptureController,
  noinline onCaptured: (ImageBitmap?, Throwable?) -> Unit,
  crossinline content: @Composable () -> Unit,
  context: Context
) = apply {
  setContent {
    content()
    LaunchedEffect(controller, onCaptured) {
      controller.captureRequests
        .mapNotNull { config -> drawToBitmapPostLaidOut(context, config) }
        .onEach { bitmap -> onCaptured(bitmap.asImageBitmap(), null) }
        .catch { error -> onCaptured(null, error) }
        .launchIn(this)
    }
  }
}

/**
 * Waits till this [View] is laid off and then draws it to the [Bitmap] with specified [config].
 */
private suspend fun View.drawToBitmapPostLaidOut(context: Context, config: Bitmap.Config): Bitmap {
  return suspendCoroutine { continuation ->
    doOnLayout { view ->
      try {
        // Initially, try to capture bitmap using drawToBitmap extension function
        continuation.resume(view.drawToBitmap(config))
      } catch (e: IllegalArgumentException) {
        // For device with API version O(26) and above should draw Bitmap using PixelCopy
        // API. The reason behind this is it throws IllegalArgumentException saying
        // "Software rendering doesn't support hardware bitmaps"
        // See this issue for the reference:
        // https://github.com/PatilShreyas/Capturable/issues/7
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          val window = context.findActivity().window

          drawBitmapWithPixelCopy(
            view = view,
            window = window,
            config = config,
            onDrawn = { bitmap -> continuation.resume(bitmap) },
            onError = { error -> continuation.resumeWithException(error) }
          )
        } else {
          continuation.resumeWithException(e)
        }
      }
    }
  }
}

/**
 * Draws a [view] to a [Bitmap] with [config] using a [PixelCopy] API.
 * Gives callback [onDrawn] after successfully drawing Bitmap otherwise invokes [onError].
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun drawBitmapWithPixelCopy(
  view: View,
  window: Window,
  config: Bitmap.Config,
  onDrawn: (Bitmap) -> Unit,
  onError: (Throwable) -> Unit
) {
  val width = view.width
  val height = view.height

  val bitmap = Bitmap.createBitmap(width, height, config)

  val (x, y) = IntArray(2).apply { view.getLocationInWindow(this) }
  val rect = Rect(x, y, x + width, y + height)

  PixelCopy.request(
    window,
    rect,
    bitmap,
    { copyResult ->
      if (copyResult == PixelCopy.SUCCESS) {
        onDrawn(bitmap)
      } else {
        onError(RuntimeException("Failed to draw bitmap"))
      }
    },
    Handler(Looper.getMainLooper())
  )
}

/**
 * Traverses through this [Context] and finds [Activity] wrapped inside it.
 */
internal fun Context.findActivity(): Activity {
  var context = this
  while (context is ContextWrapper) {
    if (context is Activity) return context
    context = context.baseContext
  }
  throw IllegalStateException("Unable to retrieve Activity from the current context")
}


class CaptureController internal constructor() {

  /**
   * Medium for providing capture requests
   */
  private val _captureRequests = MutableSharedFlow<Bitmap.Config>(extraBufferCapacity = 1)
  internal val captureRequests = _captureRequests.asSharedFlow()

  /**
   * Creates and send a Bitmap capture request with specified [config].
   *
   * Make sure to call this method as a part of callback function and not as a part of the
   * [Composable] function itself.
   *
   * @param config Bitmap config of the desired bitmap. Defaults to [Bitmap.Config.ARGB_8888]
   */
  fun capture(config: Bitmap.Config = Bitmap.Config.ARGB_8888) {
    _captureRequests.tryEmit(config)
  }
}

/**
 * Creates [CaptureController] and remembers it.
 */
@Composable
fun rememberCaptureController(): CaptureController {
  return remember { CaptureController() }
}