package org.dweb_browser.sys.window.render

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIColor
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
fun UIView.setTransform(scale: Double) {
  val viewCenter = center
  transform = CGAffineTransformMakeScale(scale, scale)
  center = viewCenter
}

fun UIView.unsetTransform() = setTransform(1.0)

@OptIn(ExperimentalForeignApi::class)
fun UIView.effectWindowFrameStyle(style: WindowFrameStyle) {
  style.opacity.toDouble().also { double ->
    alpha = double
  }

  /// 配置阴影
  layer.shadowColor = UIColor.blackColor.CGColor
  layer.shadowOffset = CGSizeMake(width = 0.0, height = style.elevation.value / 3.0)
  layer.shadowRadius = style.elevation.value.toDouble()
  layer.shadowOpacity = 0.5f

  // 配置圆角
  layer.cornerRadius = style.cornerRadius.topStart.toDouble()

  // 设置缩放
  setTransform(style.scale.toDouble())
}
