package org.dweb_browser.browser.web.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.browser.common.CaptureController
import org.dweb_browser.browser.common.CaptureParams
import org.dweb_browser.dwebview.base.ViewItem

data class BrowserContentItem(
  var bitmap: ImageBitmap? = null,
  var contentWebItem: MutableState<ContentWebItem?> = mutableStateOf(null),
  val controller: CaptureController = CaptureController(),
) {
  data class ContentWebItem(
    val viewItem: ViewItem,
    var webViewY: Int = 0,
    val loadState: MutableState<Boolean> = mutableStateOf(false),
  )

  suspend fun captureView() {
    contentWebItem.value?.let { item ->
      controller.capture(
        CaptureParams(CaptureParams.ViewType.WebView, item.webViewY, item.viewItem.webView)
      )
    } ?: run {
      controller.capture(CaptureParams.Normal)
    }
  }
}