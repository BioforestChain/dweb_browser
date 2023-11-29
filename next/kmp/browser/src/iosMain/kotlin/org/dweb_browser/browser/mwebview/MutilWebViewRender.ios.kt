package org.dweb_browser.browser.mwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.dweb_browser.dwebview.asIosWebView
import org.dweb_browser.sys.window.render.LocalWindowFrameStyle
import platform.CoreGraphics.CGAffineTransformMake

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MultiWebViewController.AfterViewItemRender(viewItem: MultiWebViewController.MultiViewItem) {
  val windowFrameStyle = LocalWindowFrameStyle.current
  LaunchedEffect(windowFrameStyle) {
    val superView = viewItem.webView.asIosWebView().superview ?: return@LaunchedEffect
    superView.alpha = windowFrameStyle.opacity.toDouble()
    val newFrame = superView.frame.useContents {
      val scale = windowFrameStyle.scale.toDouble()
      val tx = size.width * (scale - 1) / 2
      val ty = size.height * (scale - 1) / 2
      CGAffineTransformMake(scale, 0.0, 0.0, scale, tx, ty)
    }
    superView.transform = newFrame
  }
}