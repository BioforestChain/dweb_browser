package org.dweb_browser.browser.web.ui.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

@Composable
expect fun CaptureView(
  controller: CaptureController,
  modifier: Modifier = Modifier,
  onCaptured: (ImageBitmap?, Throwable?) -> Unit,
  content: @Composable () -> Unit
)

expect class CaptureController() {
  fun capture()
}

/**
 * Creates [CaptureController] and remembers it.
 */
@Composable
fun rememberCaptureController(): CaptureController {
  return remember { CaptureController() }
}
