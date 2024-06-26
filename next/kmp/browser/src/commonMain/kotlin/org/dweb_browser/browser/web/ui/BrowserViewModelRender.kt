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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.helper.capturable.capturable
import org.dweb_browser.helper.compose.IosLeaveEasing
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.withRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController

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
    Surface(Modifier.withRenderScope(windowRenderScope)) {
      // 搜索界面考虑到窗口和全屏问题，显示的问题，需要控制modifier
      if (BrowserPreviewPanel(Modifier.fillMaxSize().zIndex(2f))) return@Surface
      if (BrowserSearchPanel(Modifier.fillMaxSize().zIndex(2f))) return@Surface

      BrowserPagePanel(Modifier.fillMaxSize(), windowRenderScope.scale)
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
    HorizontalPager(modifier = Modifier.fillMaxWidth(),
      state = viewModel.pagerStates.contentPage,
      pageSpacing = 0.dp,
      userScrollEnabled = false,
      reverseLayout = false,
      contentPadding = PaddingValues(0.dp),
      beyondViewportPageCount = 1,
      pageContent = { currentPage ->
        val browserPage = viewModel.getPage(currentPage)
        browserPage.scale = contentScaled
        browserPage.Render(Modifier.fillMaxSize().capturable(browserPage.captureController))
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
