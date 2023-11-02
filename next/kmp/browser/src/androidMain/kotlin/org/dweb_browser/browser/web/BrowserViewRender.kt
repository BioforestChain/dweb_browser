package org.dweb_browser.browser.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.ktor.http.Url
import org.dweb_browser.browser.web.ui.browser.BrowserViewForWindow
import org.dweb_browser.browser.web.ui.browser.model.BrowserIntent
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController

@Composable
fun BrowserController.Render(modifier: Modifier, windowRenderScope: WindowRenderScope) {
  val controller = this
  val win = LocalWindowController.current
  val uiState = controller.viewModel.uiState
  val webViews = uiState.browserViewList.filter {
    it.viewItem.state.lastLoadedUrl?.let {
      runCatching { if (it.startsWith("https://web.browser.dweb")) null else Url(it) }
    }?.getOrNull() != null
  }

  win.GoBackHandler(
    webViews.isNotEmpty(),
    if (webViews.isNotEmpty()) "确定要关闭这 ${webViews.size} 个网页？" else null
  ) {
    val viewItem = uiState.currentBrowserBaseView.value!!.viewItem
    if (viewItem.navigator.canGoBack) {
      viewItem.navigator.navigateBack()
    } else {
      viewModel.handleIntent(BrowserIntent.RemoveBaseView(uiState.browserViewList.indexOfFirst { it.viewItem == viewItem }))
    }
  }

  BrowserViewForWindow(controller.viewModel, modifier, windowRenderScope)
}