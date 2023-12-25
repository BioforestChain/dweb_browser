package org.dweb_browser.helper.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGFloat
import platform.UIKit.UIView

fun UIView.setScale(scale: Float) = setScale(scale.toDouble())
fun UIView.setScale(scale: CGFloat) = setScale(scale, scale)

@OptIn(ExperimentalForeignApi::class)
fun UIView.setScale(scaleX: CGFloat, scaleY: CGFloat) {
  val frame = frame;
  transform = CGAffineTransformMakeScale(scaleX, scaleY)
  setFrame(frame)
}

@OptIn(ExperimentalForeignApi::class)
fun UIView.getScale() = transform.useContents { Pair(a, d) }