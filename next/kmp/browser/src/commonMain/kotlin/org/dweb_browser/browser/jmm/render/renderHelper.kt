package org.dweb_browser.browser.jmm.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

internal fun measureCenterOffset(index: Int, previewState: PreviewState): Offset {
  val firstVisible = previewState.imageLazy?.firstVisibleItemIndex ?: 0
  val firstVisibleOffset = previewState.imageLazy?.firstVisibleItemScrollOffset ?: 0
  val density = previewState.density
  val statusBarHeight = previewState.statusBarHeight
  val screenWidth = previewState.screenWidth
  val screenHeight = previewState.screenHeight
  // 计算图片中心点的坐标
  val totalTop = when (previewState.outsideLazy.firstVisibleItemIndex) {
    0 -> { // 状态栏，顶部工具栏，头部栏，评分栏
      (TopBarHeight + HeadHeight + AppInfoHeight + ImageHeight / 2 + VerticalPadding).value * density + statusBarHeight
    }

    1 -> {
      (TopBarHeight + AppInfoHeight + ImageHeight / 2 + VerticalPadding).value * density + statusBarHeight
    }

    2 -> {
      (TopBarHeight + ImageHeight / 2 + VerticalPadding).value * density + statusBarHeight
    }

    else -> {
      statusBarHeight.toFloat()
    }
  }
  val realTop =
    (totalTop - previewState.outsideLazy.firstVisibleItemScrollOffset) / (screenHeight * density)

  val realLeft = if (index > firstVisible) {
    val left1 = (HorizontalPadding + ImageWidth).value * density - firstVisibleOffset // 第一格减去移动量
    val left2 =
      (index - firstVisible - 1) * (ImageWidth + HorizontalPadding).value * density // 中间间隔多少个图片
    val left3 = (ImageWidth / 2 + HorizontalPadding).value * density // 点击的图片本身
    (left1 + left2 + left3) / (screenWidth * density)
  } else {
    val left = (ImageWidth / 2 + HorizontalPadding).value * density - firstVisibleOffset
    left / (screenWidth * density)
  }
  return Offset(realLeft, realTop)
}

@Composable
internal fun CustomerDivider(modifier: Modifier = Modifier) {
  val color = MaterialTheme.colorScheme.outlineVariant
  val thickness = 1.dp
  Canvas(modifier.fillMaxWidth().height(thickness)) {
    drawLine(
      color = color,
      strokeWidth = thickness.toPx(),
      start = Offset(0f, thickness.toPx() / 2),
      end = Offset(size.width, thickness.toPx() / 2),
    )
  }
}

fun List<String>.toContent(): String {
  val sb = StringBuilder()
  this.forEachIndexed { index, data ->
    if (index > 0) sb.append(", ")
    sb.append(data)
  }
  return sb.toString()
}