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
  val task = LoaderTask.from(fixUrl, maxWidth, maxHeight, hook)
  val result = LocalWebImageLoader.current.getLoadCache(task)// 这里的 LoadCache 和 下面的 isError-Load 做配合
    ?: LocalCoilImageLoader.current.Load(task)
  if (result.isError) {
    return LocalWebImageLoader.current.Load(task)
  }
  return result
}