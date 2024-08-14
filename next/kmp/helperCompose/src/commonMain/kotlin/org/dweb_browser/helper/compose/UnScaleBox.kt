package org.dweb_browser.helper.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 因为 NativeView 是一个外部视图，所以需要对冲缩放，来满足视图的大小合理同时确保视图的手势合理
 *
 * 比方说在android平台，如果本身处在一个被 scale 的视图中，此时直接放置webview，虽然显示正常，
 * 但是手势是错位的，所以此时需要 UnScaleBox，将视图逻辑缩小，从而让手势回到正确的位置上，而后再将视图渲染放大
 *
 * 比方说在iOS平台上，视图本身无法被正确scale，它只知道自己的place，那么需要 UnScaleBox，将视图逻辑缩小，从而避免放置视图时发生溢出
 */
@Composable
fun UnScaleBox(
  scale: Float,
  modifier: Modifier = Modifier,
  content: @Composable() (BoxScope.() -> Unit),
) {
  BoxWithConstraints(modifier) {
    val unscale = 1 / scale
    val originWidth = maxWidth / unscale
    val originHeight = maxHeight / unscale
    Box(
      modifier = Modifier.requiredSize(originWidth, originHeight).graphicsLayer(
        scaleX = unscale, scaleY = unscale, transformOrigin = TransformOrigin(0f, 0f)
      ),
      content = content,
    )
  }
}