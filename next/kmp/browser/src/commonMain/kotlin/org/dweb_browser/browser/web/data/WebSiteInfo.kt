package org.dweb_browser.browser.web.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.dwebview.base.ViewItem
import org.dweb_browser.helper.capturable.CaptureController

class BrowserContentItem {
  var bitmap: ImageBitmap? = null
    private set
  val contentWebItem: MutableState<ContentWebItem?> = mutableStateOf(null)
  val controller: CaptureController = CaptureController()

  class ContentWebItem(
    val viewItem: ViewItem,
    var webViewY: Int = 0,
    val loadState: MutableState<Boolean> = mutableStateOf(false),
  )

  suspend fun captureView() {
    bitmap = controller.captureAsync().await()
  }
}