package org.dweb_browser.browser.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.launch
import org.dweb_browser.browser.web.ui.model.BrowserViewModel
import org.dweb_browser.browser.web.ui.model.LocalShowSearchView
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController

@Composable
fun BrowserController.Render(modifier: Modifier, windowRenderScope: WindowRenderScope) {
  val controller = this
  val scope = rememberCoroutineScope()
  val win = LocalWindowController.current
  val showSearchView = LocalShowSearchView.current
  val focusManager = LocalFocusManager.current
  val webViews = controller.viewModel.listFilter()

  win.GoBackHandler(
    webViews.isNotEmpty(),
    if (webViews.isNotEmpty()) "确定要关闭这 ${webViews.size} 个网页？" else null
  ) {
    val browserWebView = controller.viewModel.currentTab ?: return@GoBackHandler
    if (showSearchView.value) { // 如果显示搜索界面，优先关闭搜索界面
      focusManager.clearFocus()
      showSearchView.value = false
    } else if (browserWebView.viewItem.webView.canGoBack()) {
      browserWebView.viewItem.webView.goBack()
    } else {
      scope.launch { controller.viewModel.removeBrowserWebView(browserWebView) }
    }
  }

  BrowserViewForWindow(controller.viewModel, modifier, windowRenderScope)
}

@Composable
expect fun BrowserViewForWindow(viewModel: BrowserViewModel, modifier: Modifier, windowRenderScope: WindowRenderScope)

