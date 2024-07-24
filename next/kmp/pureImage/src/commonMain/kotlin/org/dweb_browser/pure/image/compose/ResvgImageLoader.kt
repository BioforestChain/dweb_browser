package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureServerRequest
import org.dweb_browser.pure.http.defaultHttpPureClient
import org.dweb_browser.pure.http.fetch
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHookContext
import resvg_render.FitMode
import resvg_render.RenderOptions
import resvg_render.svgToPng

val LocalResvgImageLoader = compositionLocalOf { ResvgImageLoader.defaultInstance }

class ResvgImageLoader : PureImageLoader {
  companion object {
    val scope = globalDefaultScope
    val client = defaultHttpPureClient

    val defaultInstance = ResvgImageLoader()
  }

  @Composable
  override fun Load(task: LoaderTask): ImageLoadResult {
    return load(task).collectAsState().value
  }

  private val caches = LoaderCacheMap<MutableStateFlow<ImageLoadResult>>(scope)

  @Composable
  fun getLoadCache(task: LoaderTask): ImageLoadResult? {
    return caches.get(task)?.collectAsState()?.value
  }

  fun load(
    task: LoaderTask,
  ): StateFlow<ImageLoadResult> {
    val cache = caches.get(task)
    return cache ?: run {
      val imageResultState = MutableStateFlow(ImageLoadResult.Setup)
      val cacheItem = CacheItem(task, imageResultState)
      caches.save(cacheItem)
      scope.launch {
        runCatching {
          imageResultState.emit(ImageLoadResult.Loading)
          val pureResponse =
            task.hook?.invoke(FetchHookContext(PureServerRequest(task.url, PureMethod.GET)))
              ?: client.fetch(task.url);
          imageResultState.emit(ImageLoadResult.Loading)

          val svgData = pureResponse.binary()
          val pngData = svgToPng(
            svgData,
            RenderOptions(
              width = task.containerWidth.toFloat(),
              height = task.containerHeight.toFloat(),
              fitMode = FitMode.CONTAIN
            )
          )
          imageResultState.emit(ImageLoadResult.success(pngData.toImageBitmap()))
        }.getOrElse {
          imageResultState.emit(ImageLoadResult.error(it))
        }
      }

      imageResultState
    }
  }
}