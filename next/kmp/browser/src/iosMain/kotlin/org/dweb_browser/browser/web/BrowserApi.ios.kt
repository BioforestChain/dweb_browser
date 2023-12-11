package org.dweb_browser.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.browser.web.ui.model.BrowserViewModel
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.platform.setScale
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.render.WindowFrameStyleEffect

actual fun ImageBitmap.toImageResource(): ImageResource? = null
actual fun getImageResourceRootPath(): String = ""

public fun registerIosIMP(imp: IosInterface) = browserIosImp.registerIosIMP(imp)

//iOS的协议实现
private var browserIosImp = BrowserIosIMP()

//对外暴露的接口
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

  if (!viewModel.dwebLinkSearch.value.isEmpty()) {
    browserIosImp.doSearch(viewModel.dwebLinkSearch.value.toString())
  }

  Box {
    UIKitView(
      factory = {
        iOSView
      },
      background = Color.White,
      modifier = modifier,
    )
    iOSView.setScale(windowRenderScope.scale)
    iOSView.WindowFrameStyleEffect()
  }
}

