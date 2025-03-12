package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
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
    val loader = remember(task.key) {
      startLoad(task)
    }
    return loader.result.collectAsState().value
  }

  private val sharedLoaderResults = LoaderCacheMap<MutableStateFlow<ImageLoadResult>>(scope)

  @Composable
  fun getLoadCache(task: LoaderTask): ImageLoadResult? {
    return sharedLoaderResults.get(task)?.collectAsState()?.value
  }

  inner class TaskLoader(val task: LoaderTask, val result: MutableStateFlow<ImageLoadResult>) {
    init {
      scope.launch {
        runCatching {
          result.emit(ImageLoadResult.Loading)
          val pureResponse =
            task.hook?.invoke(FetchHookContext(PureServerRequest(task.url, PureMethod.GET)))
              ?: client.fetch(task.url);
          result.emit(ImageLoadResult.Loading)

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
            result.emit(ImageLoadResult.success(it))
          } ?: run {
            result.emit(ImageLoadResult.error(Exception("image decode fail")))
          }
        }.getOrElse {
          debugResvg("load", "fail", it)
          val failTimes = PureImageLoader.urlErrorCount.getOrPut(task.url) { 0 } + 1
          PureImageLoader.urlErrorCount[task.url] = failTimes

          result.emit(ImageLoadResult.error(it))
        }
      }

    }
  }

  fun startLoad(task: LoaderTask): TaskLoader {
    return TaskLoader(
      task,
      sharedLoaderResults.get(task) ?: MutableStateFlow(ImageLoadResult.Setup)
    ).also { loader ->
      val cacheItem = CacheItem(task, loader.result)
      sharedLoaderResults.save(cacheItem)
      scope.launch {
        loader.result.collect { result ->
          if (result.isError) {
            /// 失败后，移除执行缓存。但是这里的result仍然不会变
            if (cacheItem.result.value == loader.result) {
              sharedLoaderResults.delete(task, cacheItem)
            }
          }
        }
      }
    }
  }
}