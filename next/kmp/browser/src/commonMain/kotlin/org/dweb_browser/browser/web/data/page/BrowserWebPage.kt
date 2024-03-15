package org.dweb_browser.browser.web.data.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.dweb_browser.browser.util.isUrlOrHost
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.data.WebSiteType
import org.dweb_browser.browser.web.model.toWebSiteInfo
import org.dweb_browser.browser.web.ui.page.BrowserWebPageRender
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.withScope

class BrowserWebPage(val webView: IDWebView, private val browserController: BrowserController) :
  BrowserPage(browserController) {
  companion object {
    fun isWebUrl(url: String) = url.isUrlOrHost()
  }

  override fun isUrlMatch(url: String) = this.url == url

  override fun updateUrl(url: String) {
    super.updateUrl(url)
    webView.ioScope.launch {
      super.updateUrl(webView.loadUrl(url))
    }
  }

  // TODO 应该移除该字段，网页应该做到自己截图时只截取滚动视区，而不是通过外部提供这些滚动变量
  var scrollY: Int = 0
  var scrollX: Int = 0
  var isLoading by mutableStateOf(false)

  @Composable
  override fun Render(modifier: Modifier) {
    val viewModel = browserController.viewModel
    /// 绑定url
    DisposableEffect(this) {
      val off = webView.onReady {
        super.updateUrl(webView.getOriginalUrl())
      }
      onDispose { off() }
    }
    val uiScope = rememberCoroutineScope()
    /// 绑定URL
    LaunchedEffect(this) {
      webView.urlStateFlow.collect { url ->
        withScope(uiScope) {
          // 完成一次页面加载，保存到历史访问记录中
          viewModel.addHistoryLinkUI(webView.toWebSiteInfo(WebSiteType.History, url))
          // 更新当前url
          super.updateUrl(url)
        }
      }
    }
    /// 绑定进度
    LaunchedEffect(this) {
      webView.loadingProgressFlow.collect {
        when (it) {
          1f -> {
            isLoading = false
            /// 如果网页在后台加载，那么加载完成后，应该进行截图
            if (viewModel.focusedPage != this) {
              captureViewInBackground()
            }
          }

          else -> {
            isLoading = true
          }
        }
      }
    }

    BrowserWebPageRender(this, modifier)
  }

  override suspend fun destroy() {
    webView.destroy()
  }
}