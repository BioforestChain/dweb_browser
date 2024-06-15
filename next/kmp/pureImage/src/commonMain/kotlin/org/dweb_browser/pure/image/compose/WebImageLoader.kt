package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.pure.image.OffscreenWebCanvas
import org.dweb_browser.pure.image.offscreenwebcanvas.WebCanvasContextSession.Companion.buildTask
import org.dweb_browser.pure.image.offscreenwebcanvas.waitReady
import org.dweb_browser.pure.image.setHook


val LocalWebImageLoader = compositionChainOf("ImageLoader") { WebImageLoader.defaultInstance }

@Composable
internal expect fun rememberOffscreenWebCanvas(): OffscreenWebCanvas


class WebImageLoader : PureImageLoader {
  companion object {
    val defaultInstance by lazy { WebImageLoader() }
  }

  private val scope = globalDefaultScope

  @Composable
  override fun Load(task: LoaderTask): ImageLoadResult {
    return load(
      rememberOffscreenWebCanvas(), task
    ).collectAsState().value
  }


  private val caches = LoaderCacheMap<MutableStateFlow<ImageLoadResult>>(scope)


  @Composable
  fun LoadCache(task: LoaderTask): ImageLoadResult? {
    return caches.get(task)?.collectAsState()?.value
  }

  fun load(
    webCanvas: OffscreenWebCanvas,
    task: LoaderTask,
  ): StateFlow<ImageLoadResult> {
    return caches.get(task) ?: run {
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
          ImageLoadResult.success(imageBitmap)
        } catch (e: Throwable) {
          caches.delete(task)
          ImageLoadResult.error(e)
        } finally {
          dispose?.invoke()
        }
      }

      imageResultState
    }
  }
}
