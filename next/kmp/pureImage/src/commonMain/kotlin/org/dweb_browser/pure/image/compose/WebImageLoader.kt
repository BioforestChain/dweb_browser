package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.pure.image.OffscreenWebCanvas
import org.dweb_browser.pure.image.offscreenwebcanvas.WebCanvasContextSession.Companion.buildTask
import org.dweb_browser.pure.image.offscreenwebcanvas.waitReady
import org.dweb_browser.pure.image.setHook
import kotlin.math.min


val LocalWebImageLoader = compositionChainOf("WebImageLoader") { WebImageLoader.defaultInstance }

@Composable
@InternalComposeApi
expect fun rememberOffscreenWebCanvas(): OffscreenWebCanvas


class WebImageLoader : PureImageLoader {
  companion object {
    val defaultInstance by lazy { WebImageLoader() }
  }

  private val scope = globalDefaultScope

  @OptIn(InternalComposeApi::class)
  @Composable
  override fun Load(task: LoaderTask): ImageLoadResult {
    return load(rememberOffscreenWebCanvas(), task).collectAsState().value
  }


  private val caches = LoaderCacheMap<MutableStateFlow<ImageLoadResult>>(scope)


  @Composable
  fun getLoadCache(task: LoaderTask): ImageLoadResult? {
    return caches.get(task)?.collectAsState()?.value
  }

  fun load(
    webCanvas: OffscreenWebCanvas,
    task: LoaderTask,
  ): StateFlow<ImageLoadResult> {
    val cache = caches.get(task)
    return cache ?: run {
      val imageResultState = MutableStateFlow(ImageLoadResult.Setup)
      val cacheItem = CacheItem(task, imageResultState)
      caches.save(cacheItem)
      scope.launch {
        val dispose = task.hook?.let { webCanvas.setHook(task.url, it) }
        imageResultState.value = try {
          webCanvas.waitReady()
          imageResultState.value = ImageLoadResult.Loading;
          val imageBitmap = webCanvas.buildTask(task.url) {
            prepareImage(task.url)
            returnRequestCanvas {
              renderImage(task.url, task.containerWidth, task.containerHeight)
              returnImageBitmap()
            }
            execToImageBitmap()
          }
          PureImageLoader.urlErrorCount.remove(task.url)
          imageBitmap?.let {
            ImageLoadResult.success(it)
          } ?: run {
            ImageLoadResult.error(Throwable("image is null"))
          }
        } catch (e: Throwable) {
          val failTimes = PureImageLoader.urlErrorCount.getOrPut(task.url) { 0 } + 1
          PureImageLoader.urlErrorCount[task.url] = failTimes

          ImageLoadResult.error(e).also { res ->
            launch {
              /// 失败后，定时删除缓存。失败的次数越多，定时越久
              delay(min(failTimes * failTimes * 1000L, 30000L)) // 1 4 9 16 25 30 30 30
              if (cacheItem.result.value == res) {
                caches.delete(task, cacheItem)
              }
            }
          }
        } finally {
          dispose?.invoke()
        }
      }

      imageResultState
    }
  }
}
