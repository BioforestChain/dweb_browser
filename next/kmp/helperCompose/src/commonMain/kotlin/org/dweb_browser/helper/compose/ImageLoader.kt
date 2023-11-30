package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.OffscreenWebCanvas
import org.dweb_browser.helper.platform.offscreenwebcanvas.FetchHook
import org.dweb_browser.helper.platform.offscreenwebcanvas.WebCanvasContextSession.Companion.buildTask
import org.dweb_browser.helper.platform.offscreenwebcanvas.waitReady
import org.dweb_browser.helper.platform.setHook


val LocalImageLoader = compositionLocalOf { ImageLoader() }

@Composable
internal expect fun rememberOffscreenWebCanvas(): OffscreenWebCanvas


class ImageLoader {
  @Composable
  fun Load(
    url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook? = null
  ): ImageLoadResult {
    val density = LocalDensity.current.density
    val containerWidth = (maxWidth.value * density).toInt()
    val containerHeight = (maxHeight.value * density).toInt()
    return Load(url, containerWidth, containerHeight, hook)
  }


  private data class CacheItem(
    val url: String,
    val containerWidth: Int,
    val containerHeight: Int,
    val hook: (FetchHook)?,
    private val _result: @Composable () -> ImageLoadResult,
  ) {
    val key =
      "url=$url; containerWidth=$containerWidth; containerHeight=$containerHeight; hook=$hook"
    val result get() = _result.also { hot = 30f }
    internal var hot = 0f
  }

  /**
   * 缓存器，每隔一秒钟对所有对缓存对象进行检查，对于已经长时间没有访问的缓存对象，进行删除
   */
  private class CacheMap {
    private val map = SafeHashMap<String, CacheItem>()
    private val scope = CoroutineScope(ioAsyncExceptionHandler)

    init {
      scope.launch {
        while (true) {
          delay(1000)
          for ((key, cache) in map) {
            cache.hot -= 1f
            if (cache.hot <= 0) {
              map.remove(key)
            }
          }
        }
      }
    }

    fun get(
      url: String,
      containerWidth: Int,
      containerHeight: Int,
      hook: (FetchHook)?
    ): (@Composable () -> ImageLoadResult)? {
      val key =
        "url=$url; containerWidth=$containerWidth; containerHeight=$containerHeight; hook=$hook"
      return map[key]?.result
    }

    fun save(cache: CacheItem) {
      map[cache.key] = cache
    }

  }

  private val caches = CacheMap()

  @Composable
  fun Load(
    url: String, containerWidth: Int, containerHeight: Int, hook: (FetchHook)? = null
  ): ImageLoadResult {
    caches.get(url, containerWidth, containerHeight, hook)?.also {
      return@Load it()
    }
    val webCanvas = rememberOffscreenWebCanvas();
    val imageBitmap = @Composable {
      produceState(ImageLoadResult.Setup) {
        val dispose = if (hook != null) {
          webCanvas.setHook(url, hook)
        } else null
        value = try {
          webCanvas.waitReady()
          value = ImageLoadResult.Rendering;
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
      }.value
    }
    return imageBitmap.also {
      caches.save(CacheItem(url, containerWidth, containerHeight, hook, it))
    }()
  }
}

class ImageLoadResult(
  val success: ImageBitmap? = null,
  val error: Throwable? = null,
  val busy: String? = null,
) {
  companion object {

    internal fun success(success: ImageBitmap) = ImageLoadResult(success = success)
    internal fun error(error: Throwable?) = ImageLoadResult(error = error)

    internal val Setup = ImageLoadResult(busy = "setup...")
    internal val Rendering = ImageLoadResult(busy = "loading and rendering...")
  }

  val isSuccess get() = success != null
  inline fun with(
    onBusy: (String) -> Unit = {},
    onError: (Throwable) -> Unit = {},
    onSuccess: (ImageBitmap) -> Unit = {},
  ) {
    if (success != null) {
      onSuccess(success)
    } else if (error != null) {
      onError(error)
    } else if (busy != null) {
      onBusy(busy)
    }
  }
}