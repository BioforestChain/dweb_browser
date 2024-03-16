package org.dweb_browser.browser.web.data.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.dweb_browser.browser.util.isUrlOrHost
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.ui.page.BrowserWebPageRender
import org.dweb_browser.dwebview.IDWebView

class BrowserWebPage(val webView: IDWebView, private val browserController: BrowserController) :
  BrowserPage(browserController) {
  companion object {
    fun isWebUrl(url: String) = url.isUrlOrHost()
  }

  override fun isUrlMatch(url: String) = this.url == url

  override fun updateUrl(url: String) {
    superUpdateUrl(url)
    webView.ioScope.launch {
      superUpdateUrl(webView.loadUrl(url))
    }
  }

  override fun requestRefresh() {
    webView.requestRefresh()
  }
  internal inline fun superUpdateUrl(url: String) {
    super.updateUrl(url)
  }

  internal var scrollY by mutableIntStateOf(0)
  internal var scrollX by mutableIntStateOf(0)
//  override val previewContent: Painter?
//    @Composable get() = remember(scrollX, scrollY, thumbnail) {
//      thumbnail?.let {
//        BitmapPainter(it, IntOffset(scrollX, scrollY))
//      }
//    }

  var isLoading by mutableStateOf(false)

  @Composable
  override fun Render(modifier: Modifier) {
    BrowserWebPageRender(modifier)
  }

  override suspend fun destroy() {
    webView.destroy()
  }
}