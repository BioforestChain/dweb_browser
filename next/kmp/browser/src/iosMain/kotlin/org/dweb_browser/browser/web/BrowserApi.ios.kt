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
import platform.UIKit.UILabel
import platform.UIKit.UIView

actual fun ImageBitmap.toImageResource(): ImageResource? = null
actual fun getImageResourceRootPath(): String = ""


public var iOSMainView: (() -> UIView)? = null
public var doSearch: ((String) -> Unit)? = null
public var _browserViewModel: BrowserViewModel? = null

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CommonBrowserView(
  viewModel: BrowserViewModel,
  modifier: Modifier,
  windowRenderScope: WindowRenderScope
) {

  val iOSView = remember {
    if (iOSMainView != null) {
      iOSMainView!!()
    } else {
      UILabel().also {
        it.text = "iOS Load Fail"
      }
    }
  }

  doSearch?.let {
    if (!viewModel.dwebLinkSearch.value.isEmpty()) {
      println("MMMM send iOS key:${viewModel.dwebLinkSearch.value}")
      it(viewModel.dwebLinkSearch.value.toString())
    }
  }
  _browserViewModel = viewModel


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

