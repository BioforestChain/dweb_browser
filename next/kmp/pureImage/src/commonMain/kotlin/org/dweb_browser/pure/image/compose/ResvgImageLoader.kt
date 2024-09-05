package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.compose.toCssRgba
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.isMobile
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureServerRequest
import org.dweb_browser.pure.http.defaultHttpPureClient
import org.dweb_browser.pure.http.ext.FetchHookContext
import org.dweb_browser.pure.http.fetch
import resvg_render.FitMode
import resvg_render.RenderOptions
import resvg_render.svgToPng
import kotlin.math.min

val LocalResvgImageLoader = compositionLocalOf { ResvgImageLoader.defaultInstance }
val debugResvg = Debugger("resvg")

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

          val svgData = when (val currentColor = task.currentColor) {
            null -> pureResponse.binary()
            else -> {
              val svgCode = pureResponse.text()
              if (svgCode.contains("currentColor")) {
                svgCode.replace("currentColor", currentColor.toCssRgba()).utf8Binary
              } else pureResponse.binary()
            }
          }
          val pngData = svgToPng(
            svgData,
            RenderOptions(
              width = task.containerWidth.toFloat(),
              height = task.containerHeight.toFloat(),
              /// 移动端不超过 3mb，桌面端不超过 6mb
              layerLimitSize = if (IPureViewController.isMobile) 3145728f else 6291456f,
              fitMode = FitMode.CONTAIN
            )
          )
          pngData.toImageBitmap()?.let {
            imageResultState.emit(ImageLoadResult.success(it))
          } ?: run {
            imageResultState.emit(ImageLoadResult.error(Exception("image decode fail")))
          }
        }.getOrElse {
          debugResvg("load", "fail", it)
          val failTimes = PureImageLoader.urlErrorCount.getOrPut(task.url) { 0 } + 1
          PureImageLoader.urlErrorCount[task.url] = failTimes

          imageResultState.emit(ImageLoadResult.error(it).also { res ->
            launch {
              /// 失败后，定时删除缓存。失败的次数越多，定时越久
              delay(min(failTimes * failTimes * 1000L, 30000L)) // 1 4 9 16 25 30 30 30
              if (cacheItem.result.value == res) {
                caches.delete(task, cacheItem)
              }
            }
          })
        }
      }

      imageResultState
    }
  }
}