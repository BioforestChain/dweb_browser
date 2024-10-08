package org.dweb_browser.browser.web.ui.page

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.common.WindowControllerBinding
import org.dweb_browser.browser.web.data.WebSiteType
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.page.BrowserWebPage
import org.dweb_browser.browser.web.model.toWebSiteInfo
import org.dweb_browser.dwebview.RenderWithScale
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.isDesktop
import org.dweb_browser.helper.withScope
import org.dweb_browser.sys.window.core.LocalWindowController

@Composable
internal fun BrowserWebPage.Effect() {
  val webPage = this
  val viewModel = LocalBrowserViewModel.current
  val uiScope = rememberCoroutineScope()

  /// 和窗口控制器的绑定
  webPage.webView.WindowControllerBinding()

  /// 绑定title
  webView.titleFlow.collectAsState().value.also {
    title = it.ifEmpty {
      when (webView.loadingProgressFlow.collectAsState().value) {
        1f -> BrowserI18nResource.Web.page_title()
        else -> BrowserI18nResource.Web.web_page_loading()
      }
    }
  }

  /// 绑定URL
  LaunchedEffect(webPage) {
    webView.urlStateFlow.collect { url ->
      withScope(uiScope) {
        // 完成一次页面加载，保存到历史访问记录中
        viewModel.addHistoryLinkUI(webView.toWebSiteInfo(WebSiteType.History, url))
        // 更新当前url
        superUpdateUrl(url)
      }
    }
  }

  /// 返回按钮拦截
  key(viewModel) {
    val canGoBack by webView.canGoBackStateFlow.collectAsState()
    val enable = viewModel.focusedPage == webPage
    // 先判断是否聚焦，如果聚焦了，必定是可以返回的，在返回的时候判断是webview返回，还是关闭WebPage
    LocalWindowController.current.navigation.GoBackHandler(enable) {
      if (canGoBack) {
        webView.goBack()
      } else {
        viewModel.closePageUI(webPage)
      }
    }
  }
}

@Composable
internal fun BrowserWebPage.BrowserWebPageRender(modifier: Modifier) {
  val webPage = this
  webPage.Effect()
  val viewModel = webPage.browserController.viewModel
//  val focusedPage = viewModel.focusedPage
//  if (focusedPage != webPage) {
//    LaunchedEffect(Unit) {
//      delay(200)
//      webPage.captureViewInBackground("web view invisible")
//    }
//  }
  /**
   * 默认情况下这个WebView默认一直显示，但是桌面端例外，因为它的SwingPanel是置顶显示的，所以浏览器界面会一直盖在其它界面上面
   * 所以在这种情况下，我们显示截图就好
   */
  if (IPureViewController.isDesktop &&
    (viewModel.previewPanel.isPreviewVisible || viewModel.searchPanel.showPanel)
  ) {
    BoxWithConstraints(modifier) {
      webPage.PreviewRender(containerWidth = maxWidth, modifier = Modifier.fillMaxSize())
    }
  } else {
    ///
    webView.WindowControllerBinding()
    webView.RenderWithScale(scale, modifier)
  }
}
