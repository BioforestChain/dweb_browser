package info.bagen.dwebbrowser.microService.browser.jmm.render.appinstall

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import java.text.DecimalFormat


internal suspend fun measureCenterOffset(index: Int, previewState: PreviewState): Offset {
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
internal fun CustomerDivider(modifier: Modifier = Modifier) =
  HorizontalDivider(modifier = modifier.padding(horizontal = HorizontalPadding), color = MaterialTheme.colorScheme.background)

internal fun String.displayDownLoad(total: Long, progress: Long): String {
  val GB = 1024 * 1024 * 1024 // 定义GB的计算常量
  val MB = 1024 * 1024 // 定义MB的计算常量
  val KB = 1024 // 定义KB的计算常量
  val df = DecimalFormat("0.0");//格式化小数
  val dValue: String
  val totalValue = if (total / GB >= 1) {
    dValue = df.format(progress.toFloat() / GB)
    df.format(total.toFloat() / GB) + " GB";
  } else if (total.toFloat() / MB >= 1) {
    dValue = df.format(progress.toFloat() / MB)
    df.format(total.toFloat() / MB) + " MB";
  } else if (total.toFloat() / KB >= 1) { //如果当前Byte的值大于等于1KB
    dValue = df.format(progress.toFloat() / KB)
    df.format(total.toFloat() / KB) + " KB";
  } else {
    dValue = "$progress"
    "$total B";
  }
  return if (dValue.isEmpty()) "$this ($totalValue)" else "$this ($dValue/$totalValue)"
}

internal fun Number.toSpaceSize() = toString().toSpaceSize()
internal fun String.toSpaceSize(): String {
  if (this.isEmpty()) return "0"
  val size = this.toFloat()
  val GB = 1024 * 1024 * 1024 // 定义GB的计算常量
  val MB = 1024 * 1024 // 定义MB的计算常量
  val KB = 1024 // 定义KB的计算常量
  val df = DecimalFormat("0.0");//格式化小数
  return if (size / GB >= 1) {
    df.format(size / GB) + " GB";
  } else if (size / MB >= 1) {
    df.format(size / MB) + " MB";
  } else if (size / KB >= 1) { //如果当前Byte的值大于等于1KB
    df.format(size / KB) + " KB";
  } else {
    "$size B";
  }
}

fun List<String>.toContent(): String {
  val sb = StringBuffer()
  this.forEachIndexed { index, data ->
    if (index > 0) sb.append(", ")
    sb.append(data)
  }
  return sb.toString()
}