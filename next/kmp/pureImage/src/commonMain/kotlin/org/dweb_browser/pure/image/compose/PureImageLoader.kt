package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook

@Composable
expect fun PureImageLoader.Companion.SmartLoad(
  url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook? = null,
): ImageLoadResult

@Composable
internal fun PureImageLoader.Companion.NativeSmartLoad(
  url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook? = null,
): ImageLoadResult {
  var fixUrl = url
  if (fixUrl.startsWith("data://localhost/")) {
    fixUrl = fixUrl.replace("data://localhost/", "data:")
  }

  val task = LoaderTask.from(fixUrl, maxWidth, maxHeight, hook)
  var preferenceResult: ImageLoadResult? = null
  // 如果是 svg，使用 coil-engine 先进行快速渲染，使用 web-engine 来确保正确渲染
  if (fixUrl.endsWith(".svg") || fixUrl.startsWith("data:image/svg+xml;")) {
    preferenceResult = LocalWebImageLoader.current.Load(task)
    if (preferenceResult.isSuccess) {
      return preferenceResult
    }
  }

  val result = LocalWebImageLoader.current.LoadCache(task)// 这里的 LoadCache 和 下面的 isError-Load 做配合
    ?: LocalCoilImageLoader.current.Load(task)
  if (result.isError && preferenceResult == null) {
    return LocalWebImageLoader.current.Load(task)
  }
  return result
}

interface PureImageLoader {
  @Composable
  fun Load(task: LoaderTask): ImageLoadResult

  companion object
}

data class LoaderTask(
  val url: String,
  val containerWidth: Int,
  val containerHeight: Int,
  val hook: FetchHook?,
) {
  val key =
    "url=$url; containerWidth=$containerWidth; containerHeight=$containerHeight; hook=${hook.hashCode()}"

  companion object {
    @Composable
    fun from(
      url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook? = null,
    ): LoaderTask {
      val density = LocalDensity.current.density
      return remember(url, maxWidth, maxHeight, hook) {

        // 这里直接计算应该会比remember来的快
        val containerWidth = (maxWidth.value * density).toInt()
        val containerHeight = (maxHeight.value * density).toInt()
        LoaderTask(url, containerWidth, containerHeight, hook)
      }
    }
  }
}
