package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook

@Composable
actual fun PureImageLoader.Companion.SmartLoad(
  url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook?,
): ImageLoadResult {
  var fixUrl = url
  if (fixUrl.startsWith("data://localhost/")) {
    fixUrl = fixUrl.replace("data://localhost/", "data:")
  }
  // svg 尝试优先使用 WebImageLoader，但不强制
  if (fixUrl.endsWith(".svg")) {
    return LocalWebImageLoader.current.Load(fixUrl, maxWidth, maxHeight, hook)
  }
  val result = LocalCoilImageLoader.current.Load(fixUrl, maxWidth, maxHeight, hook)
  if (result.isError) {
    return LocalWebImageLoader.current.Load(fixUrl, maxWidth, maxHeight, hook)
  }
  return result
}