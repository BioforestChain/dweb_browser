package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGAffineTransformMake
import platform.CoreGraphics.CGRect
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
private fun UIView.setTransform(
  scale: Double,
  translateX: Double,
  translateY: Double,
) {
  transform.useContents {
    println("$a == $scale && $b == 0.0 && $c == 0.0 && $tx == $translateX && $ty == $translateY")
    a == scale && b == 0.0 && c == 0.0 && tx == translateX && ty == translateY
  }

  if (transform.useContents { a == scale && b == 0.0 && c == 0.0 && tx == translateX && ty == translateY }) {
    return
  }
  transform = CGAffineTransformMake(scale, 0.0, 0.0, scale, translateX, translateY)
}

private fun UIView.unsetTransform() = setTransform(1.0, 0.0, 0.0)

@OptIn(ExperimentalForeignApi::class)
private fun UIView.effectWindowFrameStyle(style: WindowCommonStyle) {
  style.opacity.toDouble().also { double ->
    if (alpha != double) {
      alpha = double
    }
  }
  val scale = style.scale.toDouble()
  val tx: Double
  val ty: Double
  frame.useContents {
    tx = size.width * (scale - 1) / 2
    ty = size.height * (scale - 1) / 2
  }

  setTransform(scale, tx, ty)
}

/**
 * TODO 目前只能和 IDWebView 配合使用。普通UIView如果要使用，需要注意的是，这里只是改动 superview 的 视觉上的scale，而子 view 的大小还是原本那么大，这就有问题，会导致触摸错位
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
fun <T : UIView> T.UIKitViewInWindow(
  modifier: Modifier,
  style: WindowCommonStyle,
  onInit: (T) -> Unit = { },
  update: (T) -> Unit = { },
  background: Color = Color.Unspecified,
  onRelease: (T) -> Unit = { },
  onResize: (view: T, rect: CValue<CGRect>) -> Unit = { view, rect -> view.setFrame(rect) },
  interactive: Boolean = true,
  accessibilityEnabled: Boolean = true,
) {
  remember(style) {
    superview?.effectWindowFrameStyle(style)
  }

  UIKitView(
    factory = {
      onInit(this)
      this
    },
    modifier = modifier,
    update = update,
    onResize = { view, rect ->
      superview?.effectWindowFrameStyle(style)
      onResize(this, rect)
    },
    onRelease = {
      superview?.unsetTransform()
      onRelease(this)
    },
    interactive = interactive,
    accessibilityEnabled = accessibilityEnabled,
  )
}