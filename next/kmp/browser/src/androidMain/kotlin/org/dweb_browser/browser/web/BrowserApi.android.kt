package org.dweb_browser.browser.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.common.barcode.LocalQRCodeModel
import org.dweb_browser.browser.common.barcode.QRCodeScanModel
import org.dweb_browser.browser.common.barcode.QRCodeScanView
import org.dweb_browser.browser.common.barcode.QRCodeState
import org.dweb_browser.browser.common.barcode.openDeepLink
import org.dweb_browser.browser.web.model.DESK_WEBLINK_ICONS
import org.dweb_browser.browser.web.ui.BrowserBottomSheet
import org.dweb_browser.browser.web.ui.BrowserMultiPopupView
import org.dweb_browser.browser.web.ui.BrowserSearchView
import org.dweb_browser.browser.web.ui.BrowserViewBottomBar
import org.dweb_browser.browser.web.ui.BrowserViewContent
import org.dweb_browser.browser.web.ui.bottomsheet.LocalModalBottomSheet
import org.dweb_browser.browser.web.ui.bottomsheet.ModalBottomModel
import org.dweb_browser.browser.web.ui.bottomsheet.SheetState
import org.dweb_browser.browser.web.ui.dimenBottomHeight
import org.dweb_browser.browser.web.ui.model.BrowserViewModel
import org.dweb_browser.browser.web.ui.model.LocalBrowserPageState
import org.dweb_browser.browser.web.ui.model.LocalWebViewInitialScale
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.BitmapUtil
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController

actual fun ImageBitmap.toImageResource(): ImageResource? {
  val context = NativeMicroModule.getAppContext()
  return BitmapUtil.saveBitmapToIcons(context, this.asAndroidBitmap())?.let { src ->
    ImageResource(src = "$DESK_WEBLINK_ICONS$src")
  }
}

actual fun getImageResourceRootPath(): String {
  return NativeMicroModule.getAppContext().filesDir.absolutePath + "/icons"
}

@Composable
actual fun BrowserViewForWindow(
  viewModel: BrowserViewModel, modifier: Modifier, windowRenderScope: WindowRenderScope
) {
  val scope = rememberCoroutineScope()
  val browserPagerState = viewModel.rememberBrowserPagerState()
  val initialScale = windowRenderScope.scale
  val modalBottomModel = remember { ModalBottomModel(mutableStateOf(SheetState.PartiallyExpanded)) }
  val qrCodeScanModel = remember { QRCodeScanModel() }

  CompositionLocalProvider(
    LocalModalBottomSheet provides modalBottomModel,
    LocalWebViewInitialScale provides initialScale,
    LocalBrowserPageState provides browserPagerState,
    LocalQRCodeModel provides qrCodeScanModel,
  ) {
    val win = LocalWindowController.current
    win.GoBackHandler {
      val watcher = viewModel.currentTab?.closeWatcher
      if (watcher?.canClose == true) {
        scope.launch {
          watcher.close()
        }
      } else {
        viewModel.currentTab?.viewItem?.webView?.let { webView ->
          if (webView.canGoBack()) {
            webView.goBack()
          }
        }
      }
    }

    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = dimenBottomHeight * windowRenderScope.scale)
      ) {
        BrowserViewContent(viewModel)   // 中间主体部分
      }
      Box(modifier = with(windowRenderScope) {
        if (win.isMaximized()) {
          Modifier.fillMaxSize()
        } else {
          Modifier
            .requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
            .scale(scale)
        }
      }) {
        BrowserViewBottomBar(viewModel) // 工具栏，包括搜索框和导航栏
        BrowserMultiPopupView(viewModel)// 用于显示多界面
        // 搜索界面考虑到窗口和全屏问题，显示的问题，需要控制modifier
        BrowserSearchView(
          viewModel = viewModel,
          modifier = if (win.isMaximized()) {
            Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.background)
              .size(windowRenderScope.widthDp, windowRenderScope.heightDp)
              .align(Alignment.BottomCenter)
          } else {
            Modifier.fillMaxWidth()
              .background(MaterialTheme.colorScheme.background)
              .align(Alignment.BottomCenter)
          },
          windowRenderScope = windowRenderScope
        )
        BrowserBottomSheet(viewModel)
        QRCodeScanView(
          qrCodeScanModel = qrCodeScanModel,
          onSuccess = {
            openDeepLink(it)
            scope.launch { qrCodeScanModel.stateChange.emit(QRCodeState.Hide) }
          },
          onCancel = { scope.launch { qrCodeScanModel.stateChange.emit(QRCodeState.Hide) } }
        )
      }
    }
  }
}