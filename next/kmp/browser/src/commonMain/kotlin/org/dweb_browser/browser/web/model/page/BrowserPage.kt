package org.dweb_browser.browser.web.model.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Bookmarks
import androidx.compose.material.icons.twotone.Download
import androidx.compose.material.icons.twotone.History
import androidx.compose.material.icons.twotone.PersonSearch
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.capturable.CaptureController
import org.dweb_browser.helper.compose.SimpleI18nResource

sealed class BrowserPage(browserController: BrowserController) {
  abstract fun isUrlMatch(url: String): Boolean
  open fun updateUrl(url: String) {
    this.url = url
  }

  var url by mutableStateOf("")
    internal set
  open var title by mutableStateOf("")
    internal set
  open val icon: Painter? @Composable get() = null
  open val iconColorFilter: ColorFilter? @Composable get() = null

  var scale by mutableFloatStateOf(1f)
    private set

  open fun isWebViewCompose(): Boolean = false // 用于标识是否是webview需要缩放，还是原生的compose需要缩放

  // 该字段是用来存储通过 deeplink 调用的 search 和 openinbrowser 关键字，关闭搜索界面需要直接置空
  var searchKeyWord by mutableStateOf<String?>(null)
    internal set

  /**
   * 截图器
   */
  val captureController = CaptureController()

  /**
   * 缩略图
   */
  internal var thumbnail by mutableStateOf<ImageBitmap?>(null)
  open val previewContent: Painter?
    @Composable get() = remember(thumbnail) {
      thumbnail?.let { BitmapPainter(it) }
    }

  private val captureLock = SynchronizedObject()
  private var captureJob: Job? = null

  suspend fun captureView() {
    captureViewInBackground().join()
  }

  /**
   * 用来告知界面将要刷新
   * 如果有内置的渲染器，可以override这个函数，从而辅助做到修改 thumbnail 的内容
   *
   * 比方说，IOS与Desktop是混合视图，因此可以重写这个函数，让webview进行截图，然后在 placeholderNode 背后绘制截图内容
   * Android 这是调用 view.invalidate() 从而实现onDraw的触发，从而能够被 captureController 所捕捉到这一帧发生的变化
   */
  open fun onRequestCapture(): Boolean {
    return true
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  fun captureViewInBackground() = synchronized(captureLock) {
    captureJob ?: captureController.captureAsync().also { job ->
      captureJob = job


      // 如果调用截图失败，释放相关资源
      if (!onRequestCapture()) {
        captureJob = null
      }
      job.invokeOnCompletion { error ->
        if (error == null) {
          thumbnail = job.getCompleted()
        }
        // 清理掉锁
        synchronized(captureLock) {
          if (captureJob == job) {
            captureJob = null
          }
        }
      }
    }
  }

  @Composable
  internal abstract fun Render(modifier: Modifier)

  @Composable
  fun Render(modifier: Modifier, scale: Float) {
    this.scale = scale
    if (isWebViewCompose()) {
      Render(modifier)
    } else {
      LaunchedEffect(Unit) { // 为了解决第一次截图失败问题：error=The Modifier.Node was detached
        delay(500)
        captureViewInBackground()
      }
      BoxWithConstraints(modifier = modifier) {
        Box(
          modifier = Modifier
            .requiredSize((maxWidth.value / scale).dp, (maxHeight.value / scale).dp)
            .scale(scale)
        ) {
          Render(Modifier.fillMaxSize())
        }
      }
    }
  }

  private val destroySignal = SimpleSignal()
  val onDestroy = destroySignal.toListener()
  open suspend fun destroy() {
    destroySignal.emitAndClear()
  }

  private val _isInBookmark by lazy {
    mutableStateOf(false).also { state ->
      val job = browserController.ioScope.launch {
        /// 只在这里修改，所以不用担心线程冲突，不需要走Effect
        browserController.bookmarksStateFlow.collect { bookmarks ->
          state.value = bookmarks.any { it.url == url }
        }
      }
      onDestroy {
        job.cancel()
      }
    }
  }

  /**
   * 是否在书签中
   */
  val isInBookmark get() = _isInBookmark.value
}

internal fun isMatchBaseUri(url: String, baseUri: String) = if (url == baseUri) true
else if (url.startsWith(baseUri)) url[baseUri.length].let { it == '/' || it == '?' || it == '#' }
else false

internal fun isAboutPage(url: String, name: String) =
  isMatchBaseUri(url, "chrome://$name") || isMatchBaseUri(url, "about:$name")

enum class BrowserPageType(
  val url: String, val icon: ImageVector, val title: SimpleI18nResource
) {
  Home("about:newtab", Icons.TwoTone.Star, BrowserI18nResource.Home.page_title),
  Bookmark("about:bookmarks", Icons.TwoTone.Bookmarks, BrowserI18nResource.Bookmark.page_title),
  Download("about:downloads", Icons.TwoTone.Download, BrowserI18nResource.Download.page_title),
  History("about:history", Icons.TwoTone.History, BrowserI18nResource.History.page_title),
  Engine("about:engines", Icons.TwoTone.PersonSearch, BrowserI18nResource.Engine.page_title),
  Setting("about:settings", Icons.TwoTone.Settings, BrowserI18nResource.Setting.page_title)
  ;

  @Composable
  fun iconPainter() = rememberVectorPainter(icon)

  fun isMatchUrl(url: String) = isMatchBaseUri(url, this.url)

  @Composable
  fun pageTitle() = title()
}