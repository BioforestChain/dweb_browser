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
      val result = LocalCoilImageLoader.current.Load(fixUrl, maxWidth, maxHeight, hook)
      if (result.isError) {
        return LocalWebImageLoader.current.Load(fixUrl, maxWidth, maxHeight, hook)
      }
      return result
    }
  }
}