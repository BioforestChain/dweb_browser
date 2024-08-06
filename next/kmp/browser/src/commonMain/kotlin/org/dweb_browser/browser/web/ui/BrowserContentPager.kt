package org.dweb_browser.browser.web.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
      val lazyState = viewModel.pagerStates.contentLazyState
      val focusedPageIndex = viewModel.focusedPageIndex
      LazyRow(
        state = lazyState,
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

          /// 滚动到一定阈值的时候focusedPageIndex的变更，由此来判定页面是否要离开，然后进行截图
          /// 这里的问题在于，依赖于滚动的过程，如果是snapTo，而不是animateScrollTo，那么就会有截屏失败的问题，但不影响用户体验
          /// 同时，这种依赖于 LazyRow 的方案，虽然渲染性能不错，但是Android不能在后台截图，强行获取网页位图，会有一些问题，比如滚动量未知，视窗大小未知
          /// 如果有很强烈的后台截图的需求，可以更改 lazyState 中的 prefetchStrategy 属性
          var didEnter by remember { mutableStateOf(false) }
          var willLeave by remember { mutableStateOf(false) }

          if (!didEnter) {
            if (focusedPageIndex == pageIndex) {
              didEnter = true
            }
          } else {
            if (!willLeave && focusedPageIndex != pageIndex) {
              willLeave = true
            }
          }
          if (willLeave) {
            LaunchedEffect(Unit) {
              browserPage.captureViewInBackground("page will invisible by scroll")
            }
          }
        }
      }
    }
  }
}