package info.bagen.dwebbrowser.ui.view

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume
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
        .onEach { pair: Pair<Bitmap?, Throwable?> ->
          //bitmap -> onCaptured(bitmap.asImageBitmap(), null)
          onCaptured(pair.first?.asImageBitmap(), pair.second)
        }
        .catch { error -> onCaptured(null, error) }
        .launchIn(this)
    }
  }
}

/**
 * Waits till this [View] is laid off and then draws it to the [Bitmap] with specified [config].
 */
private suspend fun View.drawToBitmapPostLaidOut(context: Context, config: Bitmap.Config):
    Pair<Bitmap?, Throwable?> {
  return suspendCoroutine { continuation ->
    doOnLayout { view ->
      try {
        // Initially, try to capture bitmap using drawToBitmap extension function
        // continuation.resume(view.drawToBitmap(config))
        continuation.resume(Pair(getBitmapFromView(view), null))
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
            // onDrawn = { bitmap -> continuation.resume(bitmap) },
            // onError = { error -> continuation.resumeWithException(error) } // 防止异常后就不再执行截屏
            onDrawn = { bitmap -> continuation.resume(Pair(bitmap, null)) },
            onError = { error -> continuation.resume(Pair(null, error)) }
          )
        } else {
          // continuation.resumeWithException(e)
          continuation.resume(Pair(null, e))
        }
      }
    }
  }
}

private fun getBitmapFromView(v: View): Bitmap? {
  /*v.isDrawingCacheEnabled = true
  v.buildDrawingCache()
  // 重新测量一遍View的宽高
  v.measure(View.MeasureSpec.makeMeasureSpec(v.width, View.MeasureSpec.EXACTLY),
    View.MeasureSpec.makeMeasureSpec(v.height, View.MeasureSpec.EXACTLY))
  // 确定View的位置
  v.layout(v.x.toInt(), v.y.toInt(), v.x.toInt() + v.measuredWidth, v.y.toInt() + v.measuredHeight)
  // 生成View宽高一样的Bitmap
  //val bmp = Bitmap.createBitmap(v.drawingCache, 0, 0, v.measuredWidth, v.measuredHeight)
  val bmp = Bitmap.createBitmap( v.measuredWidth, v.measuredHeight, Bitmap.Config.ARGB_8888)
  v.isDrawingCacheEnabled = false
  v.destroyDrawingCache()*/

  val w = v.width
  val h = v.height
  val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
  val c = Canvas(bmp)
  // 如果不设置canvas画布为白色，则生成透明
  c.drawColor(Color.WHITE)
  v.layout(0, 0, w, h)
  v.draw(c)
  return bmp
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