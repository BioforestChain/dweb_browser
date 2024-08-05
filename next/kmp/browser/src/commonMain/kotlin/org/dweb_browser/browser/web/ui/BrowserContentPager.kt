package org.dweb_browser.browser.web.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.zIndex
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.helper.capturable.capturable2
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.sys.window.render.LocalWindowController

@Composable
fun BrowserContentPager(contentScaled: Float) {
  val viewModel = LocalBrowserViewModel.current
  val localFocusManager = LocalFocusManager.current

  viewModel.focusedPage?.also { focusPage ->
    LocalWindowController.current.navigation.GoBackHandler(viewModel.pageSize > 1) {
      viewModel.closePageUI(focusPage)
    }
  }

  Box(modifier = Modifier.fillMaxSize().clickableWithNoEffect {
    localFocusManager.clearFocus()
  }) {
    /// 渲染聚焦页面
    viewModel.pagerStates.ContentPageEffect()
    BoxWithConstraints {
      LazyRow(
        state = viewModel.pagerStates.contentLazyState,
        modifier = Modifier.fillMaxWidth().zIndex(1f),
        userScrollEnabled = false,
      ) {
        items(
          viewModel.pageSize,
          key = { pageIndex -> viewModel.getPage(pageIndex).hashCode() },
        ) { pageIndex ->
          val browserPage = viewModel.getPage(pageIndex)
          browserPage.scale = contentScaled
          browserPage.Render(
            Modifier.requiredSize(maxWidth, maxHeight).capturable2(browserPage.captureController)
          )
        }
      }
    }
  }
}