package org.dweb_browser.browser.web.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.helper.compose.IosLeaveEasing
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.isDesktop
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowSurface
import squircleshape.CornerSmoothing
import squircleshape.SquircleShape

internal val dimenTextFieldFontSize = 16.sp
internal val dimenSearchInnerHorizontal = 10.dp
internal val dimenSearchInnerVertical = 8.dp
internal val dimenSearchRoundedCornerShape = 8.dp
internal val dimenShadowElevation = 2.dp
internal val dimenHorizontalPagerHorizontal = 20.dp
internal val dimenPageHorizontal = 20.dp
internal val dimenBottomHeight = 60.dp
internal val dimenSearchHeight = 40.dp
internal val dimenNavigationHeight = 40.dp
internal val tabShape = SquircleShape(30, CornerSmoothing.Small)

internal fun <T> enterAnimationSpec() = tween<T>(250, easing = IosLeaveEasing)
internal fun <T> exitAnimationSpec() = tween<T>(300, easing = IosLeaveEasing)

@Composable
fun BrowserRender(
  viewModel: BrowserViewModel, modifier: Modifier, windowRenderScope: WindowContentRenderScope,
) {
  LocalCompositionChain.current.Provider(LocalBrowserViewModel provides viewModel) {
    viewModel.ViewModelEffect(windowRenderScope)
    windowRenderScope.WindowSurface {
      /**
       * 默认情况下这个浏览器层默认一直显示，但是桌面端例外，因为它的SwingPanel是置顶显示的，所以浏览器界面会一直盖在其它界面上面
       */
      var canShowBrowserContentPager by remember { mutableStateOf(true) }
      BrowserPagePanel(
        Modifier.fillMaxSize().zIndex(1f),
        windowRenderScope.scale,
        canShowBrowserContentPager
      )
      // 搜索界面考虑到窗口和全屏问题，显示的问题，需要控制modifier
      when {
        BrowserPreviewPanel(Modifier.fillMaxSize().zIndex(2f)) -> {
          canShowBrowserContentPager = !IPureViewController.isDesktop
        }

        BrowserSearchPanel(Modifier.fillMaxSize().zIndex(2f)) -> {
          canShowBrowserContentPager = !IPureViewController.isDesktop
        }

        else -> canShowBrowserContentPager = true
      }
    }
  }
}

@Composable
fun BrowserPagePanel(
  modifier: Modifier,
  contentScaled: Float,
  canShowBrowserContentPager: Boolean,
) {
  Column(modifier) {
    // 网页主体
    Box(modifier = Modifier.weight(1f)) {
      if (canShowBrowserContentPager) {
        BrowserContentPager(contentScaled)   // 中间网页主体
      }
    }
    // 工具栏，包括搜索框和导航栏
    BrowserBottomBar(Modifier.fillMaxWidth().requiredHeight(dimenBottomHeight))
  }
}

