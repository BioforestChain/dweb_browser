package org.dweb_browser.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.dweb_browser.browser.common.barcode.QRCodeState
import org.dweb_browser.browser.web.ui.bottomsheet.SheetState
import org.dweb_browser.browser.web.ui.model.BrowserViewModel
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.NativeBackHandler
import org.dweb_browser.sys.window.render.WindowFrameStyleEffect

actual fun ImageBitmap.toImageResource(): ImageResource? = null
actual fun getImageResourceRootPath(): String = ""

public fun registerIosIMP(imp: IosInterface) = browserIosImp.registerIosIMP(imp)

//iOS的协议实现
private var browserIosImp = BrowserIosIMP()

//对iOS暴露的服务
public var browserIosService = BrowserIosService()

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CommonBrowserView(
  viewModel: BrowserViewModel,
  modifier: Modifier,
  windowRenderScope: WindowRenderScope
) {

  browserIosService.browserViewModel = viewModel

  val iOSView = remember {
    browserIosImp.createIosMainView()
  }

  DisposableEffect(viewModel) {
    val disposeVisibleBrowser = viewModel.browserOnVisible {
        browserIosImp.browserVisiable(it)
    }

    val diposeCloseBrowser = viewModel.browserOnClose {
      browserIosImp.browserClear()
    }

    onDispose {
      disposeVisibleBrowser()
      diposeCloseBrowser()
    }
  }

  if (!viewModel.dwebLinkSearch.value.isEmpty()) {
    browserIosImp.doSearch(viewModel.dwebLinkSearch.value.toString())
  }

  val win = LocalWindowController.current
  val scope = rememberCoroutineScope()

  fun backHandler() {
    if (!browserIosImp.gobackIfCanDo()) {
      scope.launch {
        win.tryCloseOrHide()
      }
    }
  }

  // 窗口返回按钮
  win.GoBackHandler { backHandler() }
  // 侧滑返回手势
  NativeBackHandler {
    if (win.isFocused()) {
      backHandler()
    }
  }

  Box {
    UIKitView(
      factory = {
        iOSView
      },
      modifier = modifier,
    )
//    iOSView.setScale(windowRenderScope.scale)
    iOSView.WindowFrameStyleEffect()
  }
}
