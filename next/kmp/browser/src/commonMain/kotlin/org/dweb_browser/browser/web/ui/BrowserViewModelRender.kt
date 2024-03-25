package org.dweb_browser.browser.web.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.dweb_browser.browser.common.barcode.BrowserQRCodeScanRender
import org.dweb_browser.browser.common.barcode.LocalQRCodeModel
import org.dweb_browser.browser.common.barcode.QRCodeScanModel
import org.dweb_browser.browser.common.barcode.QRCodeState
import org.dweb_browser.browser.common.barcode.openDeepLink
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.helper.capturable.capturable
import org.dweb_browser.helper.compose.IosFastOutSlowInEasing
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.sys.window.core.WindowContentRenderScope
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

internal fun <T> enterAnimationSpec() = tween<T>(250, easing = IosFastOutSlowInEasing)
internal fun <T> exitAnimationSpec() = tween<T>(300, easing = IosFastOutSlowInEasing)

@Composable
fun BrowserViewModalRender(
  viewModel: BrowserViewModel, modifier: Modifier, windowRenderScope: WindowContentRenderScope
) {
  val qrCodeScanModel = remember { QRCodeScanModel() }

  LocalCompositionChain.current.Provider(
    LocalBrowserViewModel provides viewModel,
    LocalQRCodeModel provides qrCodeScanModel,
  ) {
    viewModel.ViewModelEffect()
    // 窗口 BottomSheet 的按钮

//    // 窗口级返回操作
//    win.GoBackHandler {
//      val browserContentItem = viewModel.focusPage ?: return@GoBackHandler
//      scope.launch {
//        if (showSearchView.value) { // 如果显示搜索界面，优先关闭搜索界面
//          focusManager.clearFocus()
//          showSearchView.value = false
//        } else if (viewModel.showPreview) {
//          viewModel.updatePreviewState(false)
//        } else if (qrCodeScanModel.state.value != QRCodeState.Hide) {
//          qrCodeScanModel.state.value = QRCodeState.Hide
//        } else {
//          browserContentItem.contentWebItem.value?.let { contentWebItem ->
//            if (contentWebItem.viewItem.webView.canGoBack()) {
//              contentWebItem.viewItem.webView.goBack()
//            } else {
//              viewModel.closePage(browserContentItem)
//            }
//          } ?: win.hide()
//        }
//      }
//    }

    Box(modifier = remember(windowRenderScope) {
      with(windowRenderScope) {
        modifier.requiredSize((width / scale).dp, (height / scale).dp).scale(scale)
      }
    }.background(MaterialTheme.colorScheme.background)) {
      if (viewModel.isPreviewInvisible) {
        Column(Modifier.fillMaxSize()) {
          // 网页主体
          Box(modifier = Modifier.weight(1f)) {
            BrowserPageBox(windowRenderScope)   // 中间网页主体
          }
          // 工具栏，包括搜索框和导航栏
          BrowserBottomBar(Modifier.fillMaxWidth().wrapContentHeight())
        }
      }

      BrowserPreviewPanel(Modifier.zIndex(2f))
      // 搜索界面考虑到窗口和全屏问题，显示的问题，需要控制modifier
      BrowserSearchPanel(Modifier.fillMaxSize())
      BrowserQRCodeScanRender(onSuccess = {
        openDeepLink(it)
        qrCodeScanModel.updateQRCodeStateUI(QRCodeState.Hide)
      }, onCancel = { qrCodeScanModel.updateQRCodeStateUI(QRCodeState.Hide) })
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserPageBox(windowRenderScope: WindowContentRenderScope) {
  val viewModel = LocalBrowserViewModel.current
  val localFocusManager = LocalFocusManager.current

  viewModel.focusedPage?.also { focusPage ->
    LocalWindowController.current.GoBackHandler {
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
      beyondBoundsPageCount = 1,
      pageContent = { currentPage ->
        val browserPage = viewModel.getPage(currentPage)
        browserPage.Render(
          Modifier.fillMaxWidth().capturable(browserPage.captureController), windowRenderScope.scale
        )
      })
  }
}

@Composable
fun BrowserBottomBar(modifier: Modifier) {
  Box(modifier.fillMaxWidth().height(dimenBottomHeight), contentAlignment = Alignment.Center) {
    BrowserSearchBar(Modifier.fillMaxSize())
  }
}
