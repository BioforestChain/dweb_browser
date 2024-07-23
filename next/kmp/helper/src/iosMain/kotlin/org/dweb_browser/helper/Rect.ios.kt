package org.dweb_browser.helper


import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake

@OptIn(ExperimentalForeignApi::class)
fun CValue<CGRect>.toRect() = useContents {
  PureRect(
    origin.x.toFloat(),
    origin.y.toFloat(),
    size.width.toFloat(),
    size.height.toFloat()
  )
}

// 定义乘法扩展操作符
operator fun PureRect.times(density: Float): PureRect {
  return PureRect(
    x = this.x * density,
    y = this.y * density,
    width = this.width * density,
    height = this.height * density
  )
}


@OptIn(ExperimentalForeignApi::class)
fun PureRect.toIosRect() =
  CGRectMake(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())