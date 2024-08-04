package org.dweb_browser.browser.web.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.helper.capturable.capturable2
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.sys.window.render.LocalWindowController
import kotlin.math.abs

@Composable
fun BrowserHorizontalPager(contentScaled: Float) {
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
    val scope = rememberCoroutineScope()
    /// 渲染聚焦页面
    val pagerState = viewModel.pagerStates.searchBar
    HorizontalPager(modifier = Modifier.fillMaxWidth().zIndex(1f),
      state = viewModel.pagerStates.contentPage,
      pageSpacing = 0.dp,
      userScrollEnabled = false,
      reverseLayout = false,
      contentPadding = PaddingValues(0.dp),
      beyondViewportPageCount = Int.MAX_VALUE,
      pageContent = { currentPageIndex ->
        val browserPage = viewModel.getPage(currentPageIndex)
        browserPage.scale = contentScaled
        if (false) {
          val inView = abs(currentPageIndex - pagerState.targetPage) <= 1
          val debugTag = { "($currentPageIndex/${pagerState.targetPage})url=${browserPage.url}" }

          var inCapture by remember { mutableStateOf(false) }
          LaunchedEffect(browserPage) {
            launch {
              browserPage.captureController.onCaptureStart.collect {
                inCapture = true
              }
            }
            launch {
              browserPage.captureController.onCaptureEnd.collect {
                inCapture = false
              }
            }
          }
          val shouldRender = inView || inCapture || browserPage.url.isEmpty()
          when {
            shouldRender -> {
              browserPage.Render(Modifier.fillMaxSize().capturable2(browserPage.captureController))
            }

            else -> {
              Box(Modifier.fillMaxSize())
            }
          }
        }
        browserPage.Render(Modifier.fillMaxSize().capturable2(browserPage.captureController))
      })
  }
}