package org.dweb_browser.browser.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.Signal

@Composable
expect fun CaptureView(
  controller: CaptureController,
  modifier: Modifier = Modifier,
  onCaptured: (ImageBitmap?, Throwable?) -> Unit,
  content: @Composable () -> Unit
)

class CaptureController {
  private val captureSignal: Signal<CaptureParams> = Signal()
  val onCaptureResult = captureSignal.toListener()
  suspend fun capture(captureParams: CaptureParams) {
    captureSignal.emit(captureParams)
  }
}

data class CaptureParams(
  val viewType: ViewType = ViewType.Normal,
  val webViewY: Int = 0,
  val webView: IDWebView? = null,
) {
  enum class ViewType { Normal, WebView }

  companion object {
    val Normal = CaptureParams()
  }
}

/**
 * Creates [CaptureController] and remembers it.
 */
@Composable
fun rememberCaptureController(): CaptureController {
  return remember { CaptureController() }
}
