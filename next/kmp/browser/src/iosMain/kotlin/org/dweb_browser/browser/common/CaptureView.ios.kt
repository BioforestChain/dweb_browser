package org.dweb_browser.browser.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.browser.common.CaptureController

@Composable
actual fun CaptureView(
  controller: CaptureController,
  modifier: Modifier,
  onCaptured: (ImageBitmap?, Throwable?) -> Unit,
  content: @Composable () -> Unit
) {

}


actual class CaptureController actual constructor() {
  actual fun capture() {
  }
}
