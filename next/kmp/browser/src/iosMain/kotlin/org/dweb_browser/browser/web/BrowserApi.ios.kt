package org.dweb_browser.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.dweb_browser.browser.web.ui.model.BrowserViewModel
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.render.LocalWindowFrameStyle
import platform.CoreGraphics.CGAffineTransformMake
import platform.CoreGraphics.CGFloat
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UIView

actual fun ImageBitmap.toImageResource(): ImageResource? = null
actual fun getImageResourceRootPath(): String = ""


public var iOSMainView: (() -> UIView)? = null
public var onSizeChange: ((CGFloat, CGFloat) -> Unit)? = null

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

  val w = windowRenderScope.width
  val h = windowRenderScope.height
  onSizeChange?.let { it(w.toDouble(), h.toDouble()) }

  Box {
    UIKitView(
      factory = {
        iOSView
      },
      background = Color.White,
      modifier = modifier,
      update = { view ->
        println("update:::: $view")
      }
    )
    afterViewItemRender(iOSView)
  }
}


@OptIn(ExperimentalForeignApi::class)
@Composable
fun afterViewItemRender(view: UIView) {
  val windowFrameStyle = LocalWindowFrameStyle.current
  LaunchedEffect(windowFrameStyle) {
    val superView = view.superview ?: return@LaunchedEffect
    superView.alpha = windowFrameStyle.opacity.toDouble()
    val newFrame = superView.frame.useContents {
      var scale = windowFrameStyle.scale.toDouble()
      val tx = size.width * (scale - 1) / 2
      val ty = size.height * (scale - 1) / 2
      CGAffineTransformMake(scale, 0.0, 0.0, scale, tx, ty)
    }
    superView.transform = newFrame
  }
}
