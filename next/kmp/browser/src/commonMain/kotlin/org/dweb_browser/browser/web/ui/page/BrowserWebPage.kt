package org.dweb_browser.browser.web.ui.page

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.common.loading.LoadingView
import org.dweb_browser.browser.common.toWebColorScheme
import org.dweb_browser.browser.web.data.WebSiteType
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.page.BrowserWebPage
import org.dweb_browser.browser.web.model.toWebSiteInfo
import org.dweb_browser.dwebview.Render
import org.dweb_browser.helper.withScope
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.watchedState

@Composable
internal fun BrowserWebPage.Effect() {
  val webPage = this
  val viewModel = LocalBrowserViewModel.current
  val uiScope = rememberCoroutineScope()
  /// 有的网址会改变网页到图标和标题，这里使用定时器轮训更新
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000)
      tick++
    }
  }
  /// 绑定title
  LaunchedEffect(tick) {
    title = webView.getTitle().ifEmpty { BrowserI18nResource.Web.page_title.text }
  }
  /// 每一次页面加载完成的时候，触发一次脏检查
  DisposableEffect(webView, tick) {
    val off = webView.onReady {
      tick++
    }
    onDispose { off() }
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
  /// 绑定进度
  LaunchedEffect(webPage) {
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

  /// 返回按钮拦截
  key(viewModel) {
    val canGoBack by webView.canGoBackStateFlow.collectAsState()
    LocalWindowController.current.GoBackHandler(viewModel.focusedPage == webPage && canGoBack) {
      webView.goBack()
    }
  }
}


@Composable
internal fun BrowserWebPage.BrowserWebPageRender(
  modifier: Modifier
) {
  val viewModel = LocalBrowserViewModel.current
  val uiScope = rememberCoroutineScope()
  val webPage = this
  webPage.Effect()
  ///
  BoxWithConstraints(modifier.fillMaxSize()) {
    val win = LocalWindowController.current
    val colorScheme by win.watchedState { colorScheme }
    LaunchedEffect(colorScheme) {
      webView.setPrefersColorScheme(colorScheme.toWebColorScheme())
    }

    val density = LocalDensity.current.density
    /// 同步缩放量
    LaunchedEffect(scale, maxWidth, maxHeight, density) {
      webView.setContentScale(
        scale,
        maxWidth.value,
        maxHeight.value,
        density,
      )
    }

    webView.Render(Modifier.fillMaxSize())
  }
  LoadingView(webPage.isLoading) { webPage.isLoading = false } // 先不显示加载框。
}
