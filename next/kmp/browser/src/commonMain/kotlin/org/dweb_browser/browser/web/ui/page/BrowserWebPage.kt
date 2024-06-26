package org.dweb_browser.browser.web.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import org.dweb_browser.browser.BrowserI18nResource
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

  /// 绑定title
  webView.titleFlow.collectAsState().value.also {
    title = it.ifEmpty { BrowserI18nResource.Web.page_title() }
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
          /// 如果网页在后台加载，那么加载完成后，应该进行截图
          if (viewModel.focusedPage != this) {
            captureViewInBackground()
          }
        }

        else -> {}
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
  ///
  BoxWithConstraints(modifier.fillMaxSize()) {
    val win = LocalWindowController.current
    val colorScheme by win.watchedState { colorScheme }
    LaunchedEffect(colorScheme) {
      webView.setPrefersColorScheme(colorScheme.toWebColorScheme())
    }

    val density = LocalDensity.current.density
    /// 同步缩放量
    webView.setContentScaleUnsafe(scale, maxWidth.value, maxHeight.value, density)
    val originWidth = maxWidth * scale
    val originHeight = maxHeight * scale
    // TODO 由于该模块放于缩放的模块中，导致webview自身缩放冲突了，所以这边进行了反向缩放，让webview的缩放能正常
    Box(
      modifier = Modifier.requiredSize(originWidth, originHeight).graphicsLayer(
        scaleX = 1 / scale,
        scaleY = 1 / scale,
        transformOrigin = TransformOrigin(0f, 0f)
      )
    ) {
      webView.Render(Modifier.fillMaxSize())
    }
  }
}
