package org.dweb_browser.browser.web.model.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import kotlinx.coroutines.launch
import org.dweb_browser.browser.search.SearchEngine
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.ui.page.BrowserWebPageRender
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.format
import org.dweb_browser.helper.isWebUrlOrWithoutProtocol
import org.dweb_browser.helper.toWebUrlOrWithoutProtocol

class BrowserWebPage(val webView: IDWebView, browserController: BrowserController) :
  BrowserPage(browserController) {
  companion object {
    fun isWebUrl(url: String) = url.isWebUrlOrWithoutProtocol()
  }

  internal var tick by mutableIntStateOf(1)

  override val icon: Painter?
    @Composable get() = produceState<WebIcon?>(null, tick) {
      val icon = webView.getFavoriteIcon()
      if (value?.icon != icon) {
        value = icon?.let { WebIcon(it) }
      }
    }.value?.painter

  private class WebIcon(val icon: ImageBitmap) {
    val painter = BitmapPainter(icon)
  }

  override fun isUrlMatch(url: String) = this.url == url

  override fun updateUrl(url: String) {
    // 自动补充协议头
    val safeUrl = (url.toWebUrlOrWithoutProtocol() ?: return).toString()

    superUpdateUrl(safeUrl)
    webView.ioScope.launch {
      superUpdateUrl(webView.loadUrl(safeUrl))
    }
  }

  override fun onRequestCapture(): Boolean {
    if (isDestroy) return false
    requestCaptureInCompose()
    return true
  }

  internal fun superUpdateUrl(url: String) {
    super.updateUrl(url)
  }

  var isLoading by mutableStateOf(false)

  @Composable
  override fun Render(modifier: Modifier) {
    BrowserWebPageRender(modifier)
  }

  private var isDestroy = false

  override suspend fun destroy() {
    isDestroy = true
    webView.destroy()
  }

  private val searchEngine: SearchEngine? = null

  // 根据url来搜索
  fun loadUrl(url: String) {
    // 判断 url 是否是 webUrl，如果是，直接loadUrl；如果不是，判断之前使用的搜索引擎将关键字替换了，进行搜索
    if (isWebUrl(url)) {
      updateUrl(url)
    } else {
      searchEngine?.let { updateUrl(searchEngine.searchLinks.first().format(url)) }
    }
  }
}

expect fun BrowserWebPage.requestCaptureInCompose(): Unit