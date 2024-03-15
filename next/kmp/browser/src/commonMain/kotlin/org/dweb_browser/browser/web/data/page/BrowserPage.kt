package org.dweb_browser.browser.web.data.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.capturable.CaptureController

sealed class BrowserPage(browserController: BrowserController) {
  abstract fun isUrlMatch(url: String): Boolean
  open fun updateUrl(url: String) {
    this.url = url
  }

  var url by mutableStateOf("")
    private set
  open var title by mutableStateOf("")
    internal set
  open val icon: Painter? @Composable get() = null //by mutableStateOf<Painter?>(null)
  open val iconColorFilter: ColorFilter? @Composable get() = null //by mutableStateOf<ColorFilter?>(null)

  var scale by mutableFloatStateOf(1f)

  /**
   * 截图器
   */
  val captureController = CaptureController()

  /**
   * 缩略图
   */
  var thumbnail by mutableStateOf<ImageBitmap?>(null)
    private set

  private var captureJob = atomic<Job?>(null)

  suspend fun captureView() {
    captureViewInBackground().join()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  fun captureViewInBackground() = captureJob.updateAndGet { busyJob ->
    busyJob ?: captureController.captureAsync().apply {
      invokeOnCompletion { error ->
        if (error == null) {
          thumbnail = getCompleted()
        }
        // 清理掉锁
        captureJob.update {
          if (it == this) null else it
        }
      }
    }
  }!!

  @Composable
  abstract fun Render(modifier: Modifier)

  private val destroySignal = SimpleSignal()
  val onDestroy = destroySignal.toListener()
  abstract suspend fun destroy()

  /**
   * 是否在书签中
   */
  val isInBookmark by mutableStateOf(false).also { state ->
    val job = browserController.ioScope.launch {
      browserController.bookmarks.collect { bookmarks ->
        state.value = bookmarks.any { it.url == url }
      }
    }
    onDestroy {
      job.cancel()
    }
  }
}

internal fun isMatchBaseUri(url: String, baseUri: String) = if (url == baseUri) true
else if (url.startsWith(baseUri)) url[baseUri.length].let { it == '/' || it == '?' || it == '#' }
else false

internal fun isAboutPage(url: String, name: String) =
  isMatchBaseUri(url, "chrome://$name") || isMatchBaseUri(url, "about:$name")

