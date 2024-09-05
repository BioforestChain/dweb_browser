package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import org.dweb_browser.pure.http.ext.FetchHook

@Composable
expect fun PureImageLoader.Companion.SmartLoad(
  url: String, maxWidth: Dp, maxHeight: Dp, currentColor: Color? = null, hook: FetchHook? = null,
): ImageLoadResult

private fun String.fixUrl() = when {
  startsWith("data://localhost/") -> replace("data://localhost/", "data:")
  else -> this
}

@Composable
fun PureImageLoader.Companion.CommonSmartLoad(
  url: String, maxWidth: Dp, maxHeight: Dp, currentColor: Color? = null, hook: FetchHook? = null,
): ImageLoadResult {
  val fixUrl = url.fixUrl()

  val task = LoaderTask.from(fixUrl, maxWidth, maxHeight, currentColor, hook)
  var bestResult: ImageLoadResult? = null
  // 如果是 svg，使用 coil-engine 先进行快速渲染，使用 web-engine 来确保正确渲染
  if (fixUrl.endsWith(".svg") || fixUrl.startsWith("data:image/svg+xml;")) {
    bestResult = LocalResvgImageLoader.current.Load(task)
    if (bestResult.isSuccess) {
      return bestResult
    }
  }

  val backupResult = LocalCoilImageLoader.current.Load(task)
  // 如果没有使用 webImageLoader，并且 coilImageLoader 还失败了，那么直接使用 webImageLoader 去加载
  if (backupResult.isError && bestResult == null) {
    return LocalResvgImageLoader.current.Load(task)
  }
  return backupResult
}

@Composable
fun PureImageLoader.Companion.StableSmartLoad(
  url: String, maxWidth: Dp, maxHeight: Dp, currentColor: Color? = null, hook: FetchHook? = null,
): ImageLoadResult {
  val fixUrl = url.fixUrl()

  val task = LoaderTask.from(fixUrl, maxWidth, maxHeight, currentColor, hook)
  // 如果是 svg，使用 coil-engine 先进行快速渲染，使用 web-engine 来确保正确渲染
  if (fixUrl.endsWith(".svg") || fixUrl.startsWith("data:image/svg+xml;")) {
    val result = LocalResvgImageLoader.current.Load(task)
    if (result.isError) {
      return LocalWebImageLoader.current.Load(task)
    }
    return result
  }

  val result = LocalCoilImageLoader.current.Load(task)
  if (result.isError) {
    return LocalWebImageLoader.current.Load(task)
  }
  return result
}

@Composable
fun PureImageLoader.Companion.FastSmartLoad(
  url: String, maxWidth: Dp, maxHeight: Dp, currentColor: Color? = null, hook: FetchHook? = null,
): ImageLoadResult {
  val fixUrl = url.fixUrl()

  val task = LoaderTask.from(fixUrl, maxWidth, maxHeight, currentColor, hook)
  val result =
    LocalResvgImageLoader.current.getLoadCache(task)// 这里的 LoadCache 和 下面的 isError-Load 做配合
      ?: LocalCoilImageLoader.current.Load(task)
  if (result.isError) {
    return LocalWebImageLoader.current.Load(task)
  }
  return result
}


interface PureImageLoader {
  @Composable
  fun Load(task: LoaderTask): ImageLoadResult

  companion object
}

val PureImageLoader.Companion.urlErrorCount by lazy {
  mutableMapOf<String, Int>()
}

data class LoaderTask(
  val url: String,
  val containerWidth: Int,
  val containerHeight: Int,
  val currentColor: Color?,
  val hook: FetchHook?,
) {
  val key =
    "url=$url; containerWidth=$containerWidth; containerHeight=$containerHeight; currentColor=$currentColor; hook=${hook.hashCode()}"

  companion object {
    @Composable
    fun from(
      url: String,
      maxWidth: Dp,
      maxHeight: Dp,
      currentColor: Color? = null,
      hook: FetchHook? = null,
    ): LoaderTask {
      val density = LocalDensity.current.density
      return remember(url, maxWidth, maxHeight, currentColor, hook) {
        // 这里直接计算应该会比remember来的快
        val containerWidth = (maxWidth.value * density).toInt()
        val containerHeight = (maxHeight.value * density).toInt()
        LoaderTask(url, containerWidth, containerHeight, currentColor, hook)
      }
    }
  }
}
