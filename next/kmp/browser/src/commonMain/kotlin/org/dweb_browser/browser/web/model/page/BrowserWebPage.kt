package org.dweb_browser.browser.web.model.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.ui.page.BrowserWebPageRender
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.isWebUrlOrWithoutProtocol
import org.dweb_browser.helper.toWebUrlOrWithoutProtocol

class BrowserWebPage(val webView: IDWebView, private val browserController: BrowserController) :
  BrowserPage(browserController) {
  companion object {
    fun isWebUrl(url: String) = url.isWebUrlOrWithoutProtocol()
  }

  override val icon: Painter?
    @Composable get() {
      var tick by remember { mutableStateOf(1) }
      /// 有的网址会改变图标更新，这里使用定时器轮训更新
      LaunchedEffect(tick) {
        while (true) {
          delay(1000)
          tick++
        }
      }
      /// 每一次页面加载完成的时候，触发一次图标获取
      DisposableEffect(webView, tick) {
        val off = webView.onReady {
          tick++
        }
        onDispose { off() }
      }
      return produceState<WebIcon?>(null, tick) {
        val icon = webView.getFavoriteIcon()
        if (value?.icon != icon) {
          value = icon?.let { WebIcon(it) }
        }
      }.value?.painter
    }

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
    title = BrowserI18nResource.Web.page_title()
    BrowserWebPageRender(modifier)
  }

  override suspend fun destroy() {
    webView.destroy()
  }
}