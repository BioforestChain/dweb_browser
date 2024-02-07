package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook

interface PureImageLoader {
  @Composable
  fun Load(
    url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook?
  ): ImageLoadResult

  companion object {
    @Composable
    fun SmartLoad(
      url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook? = null
    ): ImageLoadResult {
      var fixUrl = url
      if (fixUrl.startsWith("data://localhost/")) {
        fixUrl = fixUrl.replace("data://localhost/", "data:")
      }
      val coilImageLoader = LocalCoilImageLoader.current
      val result1 = coilImageLoader.Load(fixUrl, maxWidth, maxHeight, hook)
      if (result1.isError) {
        return LocalWebImageLoader.current.Load(fixUrl, maxWidth, maxHeight, hook)
      }
      return result1
    }
  }
}