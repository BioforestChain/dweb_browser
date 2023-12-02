package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGAffineTransformMake
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
@Composable
fun UIView.WindowFrameStyleEffect() {
  val windowFrameStyle = LocalWindowFrameStyle.current
  LaunchedEffect(windowFrameStyle) {
    val superView = superview ?: return@LaunchedEffect
    superView.alpha = windowFrameStyle.opacity.toDouble()
    val newTransform = superView.frame.useContents {
      val scale = windowFrameStyle.scale.toDouble()
      val tx = size.width * (scale - 1) / 2
      val ty = size.height * (scale - 1) / 2
      CGAffineTransformMake(scale, 0.0, 0.0, scale, tx, ty)
    }
    superView.transform = newTransform
  }
}