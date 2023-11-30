package org.dweb_browser.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.browser.web.ui.model.BrowserViewModel
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.platform.LocalPureViewBox
import org.dweb_browser.helper.platform.PureViewBox
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.render.WindowPreviewer
import org.dweb_browser.sys.window.render.watchedState
import platform.CoreGraphics.CGFloat
import platform.UIKit.UIView

actual fun ImageBitmap.toImageResource(): ImageResource? = null
actual fun getImageResourceRootPath(): String = ""


public var iOSMainView: UIView? = null
public var onSizeChange: ((CGFloat, CGFloat) -> Unit)? = null

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CommonBrowserView(viewModel: BrowserViewModel, modifier: Modifier, windowRenderScope: WindowRenderScope) {
  if (iOSMainView == null) {
    Text("iOS Load Fail")
  } else {

    val w = windowRenderScope.width
    val h = windowRenderScope.height
    println("Mike: w:${windowRenderScope.width} h:${windowRenderScope.height} s:${windowRenderScope.scale}")
    onSizeChange?.let { it(w.toDouble(), h.toDouble()) }

    Box {
      UIKitView(
        factory = {
          iOSMainView!!
        },
        modifier = modifier,
        update = { view ->
          println("update:::: $view")
        }
      )
    }
  }
}