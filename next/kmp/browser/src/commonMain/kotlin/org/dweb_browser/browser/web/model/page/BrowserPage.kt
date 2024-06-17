package org.dweb_browser.browser.web.model.page

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Bookmarks
import androidx.compose.material.icons.twotone.Download
import androidx.compose.material.icons.twotone.History
import androidx.compose.material.icons.twotone.PersonSearch
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.capturable.CaptureController
import org.dweb_browser.helper.compose.SimpleI18nResource
import org.dweb_browser.helper.globalDefaultScope

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

  open fun isWebViewCompose(): Boolean = false // 用于标识是否是webview需要缩放，还是原生的compose需要缩放

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

  fun captureViewInBackground() = globalDefaultScope.launch {
    val preThumbnail = thumbnail
    onRequestCapture()
    if (preThumbnail == thumbnail) {
      thumbnail = captureController.captureAsync().await()
    }
  }

  @Composable
  internal abstract fun Render(modifier: Modifier)

  private val destroySignal = SimpleSignal()
  val onDestroy = destroySignal.toListener()
  open suspend fun destroy() {
    destroySignal.emitAndClear()
  }

  private val _isInBookmark by lazy {
    mutableStateOf(false).also { state ->
      val job = browserController.lifecycleScope.launch {
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
  val url: String, val icon: ImageVector, val title: SimpleI18nResource,
) {
  Home(
    "about:newtab", Icons.TwoTone.Star, BrowserI18nResource.Home.page_title
  ),
  Bookmark(
    "about:bookmarks", Icons.TwoTone.Bookmarks, BrowserI18nResource.Bookmark.page_title
  ),
  Download(
    "about:downloads", Icons.TwoTone.Download, BrowserI18nResource.Download.page_title
  ),
  History(
    "about:history", Icons.TwoTone.History, BrowserI18nResource.History.page_title
  ),
  Engine(
    "about:engines", Icons.TwoTone.PersonSearch, BrowserI18nResource.Engine.page_title
  ),
  Setting("about:settings", Icons.TwoTone.Settings, BrowserI18nResource.Setting.page_title);

  @Composable
  fun iconPainter() = rememberVectorPainter(icon)

  fun isMatchUrl(url: String) = isMatchBaseUri(url, this.url)

  @Composable
  fun pageTitle() = title()
}