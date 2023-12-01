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
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UIView

actual fun ImageBitmap.toImageResource(): ImageResource? = null
actual fun getImageResourceRootPath(): String = ""


public var iOSMainView: (() -> UIView)? = null
public var doSearch: ((String) -> Unit)? = null

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


  Box {
    UIKitView(
      factory = {
        iOSView
      },
      background = Color.White,
      modifier = modifier,
//      update = { view ->
//        println("update:::: $view")
//      }
    )
    observerSizeAction(windowRenderScope, iOSView)
    observerScaleAction(iOSView,windowRenderScope.width,windowRenderScope.height)
  }
}


@OptIn(ExperimentalForeignApi::class)
@Composable
fun observerSizeAction(windowRenderScope: WindowRenderScope, iOSView: UIView) {
  LaunchedEffect(windowRenderScope) {
    val rect = CGRectMake(0.0,0.0,windowRenderScope.width.toDouble(), windowRenderScope.height.toDouble())
    iOSView.setFrame(rect)
  }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
fun observerScaleAction(iOSView: UIView, width: Float, height: Float) {
  val windowFrameStyle = LocalWindowFrameStyle.current
  LaunchedEffect(windowFrameStyle) {
    val superView = iOSView.superview ?: return@LaunchedEffect
    superView.alpha = windowFrameStyle.opacity.toDouble()
    var originX: CGFloat = 0.0
    var originY: CGFloat = 0.0
    val newFrame = superView.frame.useContents {
      var scale = windowFrameStyle.scale.toDouble()
      originX = this.origin.x
      originY = this.origin.y
      val tx = size.width * (scale - 1) / 2
      val ty = size.height * (scale - 1) / 2
      CGAffineTransformMake(scale, 0.0, 0.0, scale, tx, ty)
    }
    superView.transform = newFrame
    superView.setFrame(
      platform.CoreGraphics.CGRectMake(
        originX,
        originY,
        width.toDouble(),
        height.toDouble()
      )
    )
  }
}
