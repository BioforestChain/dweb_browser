package org.dweb_browser.browser.common

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun CaptureView(
  controller: CaptureController,
  modifier: Modifier,
  onCaptured: (ImageBitmap?, Throwable?) -> Unit,
  content: @Composable () -> Unit
) {
  Box {
    content()
  }
}
