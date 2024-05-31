package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.pure.image.OffscreenWebCanvas
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook
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

  private val scope = CoroutineScope(ioAsyncExceptionHandler)

  @Composable
  override fun Load(
    url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook?,
  ): ImageLoadResult {
    val density = LocalDensity.current.density
    // 这里直接计算应该会比remember来的快
    val containerWidth = (maxWidth.value * density).toInt()
    val containerHeight = (maxHeight.value * density).toInt()
    return load(
      rememberOffscreenWebCanvas(), url, containerWidth, containerHeight, hook
    ).collectAsState().value
  }


  private data class CacheItem(
    val url: String,
    val containerWidth: Int,
    val containerHeight: Int,
    val hook: (FetchHook)?,
    private val _result: StateFlow<ImageLoadResult>,
  ) {
    companion object {
      fun genKey(
        url: String,
        containerWidth: Int,
        containerHeight: Int,
        hook: (FetchHook)?,
      ) =
        "url=$url; containerWidth=$containerWidth; containerHeight=$containerHeight; hook=${hook.hashCode()}"

    }

    val key = genKey(url, containerWidth, containerHeight, hook)
    val result get() = _result.also { hot = 30f }
    internal var hot = 0f
  }

  /**
   * 缓存器，每隔一秒钟对所有对缓存对象进行检查，对于已经长时间没有访问的缓存对象，进行删除
   */
  private class CacheMap(val scope: CoroutineScope) {
    private val map = SafeHashMap<String, CacheItem>()

    init {
      scope.launch {
        while (true) {
          delay(5000)
          for ((key, cache) in map) {
            if (cache.result.value.isError) {
              map.remove(key)
            } else {
              cache.hot -= 5f
              if (cache.hot <= 0) {
                map.remove(key)
              }
            }
          }
        }
      }
    }

    fun get(
      url: String,
      containerWidth: Int,
      containerHeight: Int,
      hook: (FetchHook)?,
    ): StateFlow<ImageLoadResult>? {
      val key = CacheItem.genKey(url, containerWidth, containerHeight, hook)

      return map[key]?.result
    }

    fun save(cache: CacheItem) {
      map[cache.key] = cache
    }

    fun delete(cache: CacheItem) {
      map.remove(cache.key)
    }
  }

  private val caches = CacheMap(scope)

  fun load(
    webCanvas: OffscreenWebCanvas,
    url: String, containerWidth: Int, containerHeight: Int, hook: (FetchHook)? = null,
  ): StateFlow<ImageLoadResult> {
    val hookKey = if (url.startsWith("https://") || url.startsWith("http://")) null else hook
    return caches.get(url, containerWidth, containerHeight, hookKey) ?: run {
      val imageResultState = MutableStateFlow(ImageLoadResult.Setup)
      val cacheItem = CacheItem(url, containerWidth, containerHeight, hookKey, imageResultState)
      caches.save(cacheItem)
      scope.launch {
        val dispose = hook?.let { webCanvas.setHook(url, it) }
        imageResultState.value = try {
          webCanvas.waitReady()
          imageResultState.value = ImageLoadResult.Loading;
          val imageBitmap = webCanvas.buildTask {
            renderImage(url, containerWidth, containerHeight)
            toImageBitmap()
          }
          ImageLoadResult.success(imageBitmap)
        } catch (e: Throwable) {
          ImageLoadResult.error(e)
        } finally {
          dispose?.invoke()
        }
      }

      imageResultState
    }
  }
}
