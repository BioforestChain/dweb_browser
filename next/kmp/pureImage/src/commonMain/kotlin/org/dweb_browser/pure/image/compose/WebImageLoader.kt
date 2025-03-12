package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.pure.image.OffscreenWebCanvas
import org.dweb_browser.pure.image.offscreenwebcanvas.WebCanvasContextSession.Companion.buildTask
import org.dweb_browser.pure.image.offscreenwebcanvas.waitReady
import org.dweb_browser.pure.image.setHook


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
    val webCanvas = rememberOffscreenWebCanvas()
    val loader = remember(task.key, webCanvas) {
      startLoad(task, webCanvas)
    }
    return loader.result.collectAsState().value
  }


  private val sharedLoaderResults = LoaderCacheMap<MutableStateFlow<ImageLoadResult>>(scope)


  @Composable
  fun getLoadCache(task: LoaderTask): ImageLoadResult? {
    return sharedLoaderResults.get(task)?.collectAsState()?.value
  }

  inner class TaskLoader(
    val task: LoaderTask,
    val webCanvas: OffscreenWebCanvas, val result: MutableStateFlow<ImageLoadResult>,
  ) {
    init {
      val imageResultState = this.result
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

          ImageLoadResult.error(e)
        } finally {
          dispose?.invoke()
        }
      }

    }
  }

  fun startLoad(
    task: LoaderTask,
    webCanvas: OffscreenWebCanvas,
  ): TaskLoader {
    return TaskLoader(
      task, webCanvas,
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
