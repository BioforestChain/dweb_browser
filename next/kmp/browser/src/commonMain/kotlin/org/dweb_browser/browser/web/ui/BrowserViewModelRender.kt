package org.dweb_browser.browser.web.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.helper.capturable.capturable2
import org.dweb_browser.helper.compose.IosLeaveEasing
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowSurface
import org.dweb_browser.sys.window.render.LocalWindowController
import kotlin.math.abs

internal val dimenTextFieldFontSize = 16.sp
internal val dimenSearchInnerHorizontal = 10.dp
internal val dimenSearchInnerVertical = 8.dp
internal val dimenSearchRoundedCornerShape = 8.dp
internal val dimenShadowElevation = 4.dp
internal val dimenHorizontalPagerHorizontal = 20.dp
internal val dimenPageHorizontal = 20.dp
internal val dimenBottomHeight = 60.dp
internal val dimenSearchHeight = 40.dp
internal val dimenNavigationHeight = 40.dp

internal fun <T> enterAnimationSpec() = tween<T>(250, easing = IosLeaveEasing)
internal fun <T> exitAnimationSpec() = tween<T>(300, easing = IosLeaveEasing)

@Composable
fun BrowserViewModalRender(
  viewModel: BrowserViewModel, modifier: Modifier, windowRenderScope: WindowContentRenderScope,
) {
  LocalCompositionChain.current.Provider(LocalBrowserViewModel provides viewModel) {
    viewModel.ViewModelEffect(windowRenderScope)
    windowRenderScope.WindowSurface {
      BrowserPagePanel(Modifier.fillMaxSize().zIndex(1f), windowRenderScope.scale)
      // 搜索界面考虑到窗口和全屏问题，显示的问题，需要控制modifier
      when {
        BrowserPreviewPanel(Modifier.fillMaxSize().zIndex(2f)) -> {}
        BrowserSearchPanel(Modifier.fillMaxSize().zIndex(2f)) -> {}
      }
    }
  }
}

@Composable
fun BrowserPagePanel(modifier: Modifier, contentScaled: Float) {
  Column(modifier) {
    // 网页主体
    Box(modifier = Modifier.weight(1f)) {
      BrowserPageBox(contentScaled)   // 中间网页主体
    }
    // 工具栏，包括搜索框和导航栏
    BrowserBottomBar(Modifier.fillMaxWidth().wrapContentHeight())
  }
}

@Composable
fun BrowserPageBox(contentScaled: Float) {
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
                println("QAQ Capture Start ${debugTag()}")
                inCapture = true
              }
            }
            launch {
              browserPage.captureController.onCaptureEnd.collect {
                inCapture = false
                println("QAQ Capture End ${debugTag()}")
              }
            }
          }
          val shouldRender = inView || inCapture || browserPage.url.isEmpty()
          when {
            shouldRender -> {
              println("QAQ Render On Foreground ${debugTag()}")
              browserPage.Render(Modifier.fillMaxSize().capturable2(browserPage.captureController))
            }

            else -> {
              println("QAQ Render Ignore ${debugTag()}")
              Box(Modifier.fillMaxSize())
            }
          }
        }
        browserPage.Render(Modifier.fillMaxSize().capturable2(browserPage.captureController))
      })
  }
}

@Composable
fun BrowserBottomBar(modifier: Modifier) {
  Box(
    modifier = modifier.fillMaxWidth().height(dimenBottomHeight),
    contentAlignment = Alignment.Center
  ) {
    BrowserSearchBar(Modifier.fillMaxSize())
  }
}
