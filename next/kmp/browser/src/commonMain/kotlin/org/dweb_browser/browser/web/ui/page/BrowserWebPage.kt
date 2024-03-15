package org.dweb_browser.browser.web.ui.page

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import org.dweb_browser.browser.common.loading.LoadingView
import org.dweb_browser.browser.common.toWebColorScheme
import org.dweb_browser.browser.web.data.page.BrowserWebPage
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.dwebview.Render
import org.dweb_browser.dwebview.ScrollChangeEvent
import org.dweb_browser.helper.OffListener
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.watchedState

@Composable
internal fun BrowserWebPageRender(
  webPage: BrowserWebPage, modifier: Modifier
) {
  val viewModel = LocalBrowserViewModel.current
  /// 返回按钮拦截
  key(viewModel) {
    val canGoBack by webPage.webView.canGoBackStateFlow.collectAsState()
    LocalWindowController.current.GoBackHandler(viewModel.focusedPage == webPage && canGoBack) {
      webPage.webView.goBack()
    }
  }

  BoxWithConstraints(modifier.fillMaxSize()) {
    val win = LocalWindowController.current
    val colorScheme by win.watchedState { colorScheme }
    LaunchedEffect(colorScheme) {
      webPage.webView.setPrefersColorScheme(colorScheme.toWebColorScheme())
    }

    val density = LocalDensity.current.density

    LaunchedEffect(webPage.scale, maxWidth, maxHeight, density) {
      webPage.webView.setContentScale(
        webPage.scale,
        maxWidth.value,
        maxHeight.value,
        density,
      )
    }

    var off by remember { mutableStateOf<OffListener<ScrollChangeEvent>?>(null) }

    webPage.webView.Render(modifier = Modifier.fillMaxSize(), onCreate = {
      val webView = webPage.webView
      off = webView.onScroll {
        // 用于截图的时候进行定位截图
        webPage.scrollY = it.scrollY
        webPage.scrollX = it.scrollX
      }
    }, onDispose = {
      off?.also {
        it()
        off = null
      }
    })
  }
  LoadingView(webPage.isLoading) { webPage.isLoading = false } // 先不显示加载框。
}
