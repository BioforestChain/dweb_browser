package org.dweb_browser.sys.window.helper

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.PureBounds
import org.dweb_browser.helper.compose.compositionChainOf
import squircleshape.CornerSmoothing
import squircleshape.SquircleShape

val LocalWindowFrameStyle = compositionChainOf<WindowFrameStyle>("WindowFrameStyle")

/**
 * 窗口边距布局配置
 */
data class WindowFrameStyle(
  val frameSize: PureBounds,
  /**
   * 窗口边框圆角
   */
  val frameRounded: CornerRadius,
  /**
   * 内容圆角
   */
  val contentRounded: CornerRadius,
//  /**
//   * 内容的安全绘制区域
//   */
//  val contentSafeAreaInsets: Bounds,
  /**
   * 边框的安全绘制区域
   */
  val frameSafeAreaInsets: PureBounds,
) {
  val startWidth
    @Composable get() = when {
      LocalLayoutDirection.current == LayoutDirection.Ltr -> frameSize.left
      else -> frameSize.right
    }
  val endWidth
    @Composable get() = when {
      LocalLayoutDirection.current == LayoutDirection.Ltr -> frameSize.right
      else -> frameSize.left
    }

  data class CornerRadius(
    val topStart: Float, val topEnd: Float, val bottomStart: Float, val bottomEnd: Float,
  ) {
    operator fun div(value: Float) =
      CornerRadius(topStart / value, topEnd / value, bottomStart / value, bottomEnd / value)

    val roundedCornerShape by lazy {
      SquircleShape(
        topStartCorner = CornerSize(topStart.dp),
        topEndCorner = CornerSize(topEnd.dp),
        bottomStartCorner = CornerSize(bottomStart.dp),
        bottomEndCorner = CornerSize(bottomEnd.dp),
        cornerSmoothing = CornerSmoothing.Small,
      )
    }

    companion object {
      fun from(radius: Float) = CornerRadius(radius, radius, radius, radius)
      fun from(radius: Int) = from(radius.toFloat())
      fun from(topRadius: Float, bottomRadius: Float) =
        CornerRadius(topRadius, topRadius, bottomRadius, bottomRadius)

      fun from(topRadius: Int, bottomRadius: Int) =
        from(topRadius.toFloat(), bottomRadius.toFloat())

      val Zero = from(0)
      val Tiny = from(4)
      val Small = from(8)
      val Default = from(16)
      val Medium = from(24)
      val Large = from(32)
    }
  }

  data class ContentSize(val width: Float, val height: Float)
}