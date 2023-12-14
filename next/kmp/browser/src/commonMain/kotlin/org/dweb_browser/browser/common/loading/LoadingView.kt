package org.dweb_browser.browser.common.loading

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.compose.rememberScreenSize
import org.dweb_browser.sys.window.render.NativeBackHandler
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LoadingView(show: MutableState<Boolean>) {
  if (show.value) {
    val whiteBackground = !isSystemInDarkTheme()
    LaunchedEffect(show) {
      LoadingViewModel.setBackground(whiteBackground)
      LoadingViewModel.startTimer()
    }
    DisposableEffect(LoadingViewModel) {
      // When the effect leaves the Composition, remove the observer
      onDispose {
        LoadingViewModel.timerDestroy()
      }
    }
    NativeBackHandler {
      if (show.value) show.value = false
    }
    val width = rememberScreenSize().screenWidth
    val count = 8
    val rotateAngle = (360 / count).toDouble()
    Box(
      modifier = Modifier.fillMaxSize(),
      // .background(MaterialTheme.colorScheme.background) // 背景不显示，需要看到后面内容
      //.clickableWithNoEffect { }, // 不拦截点击
      contentAlignment = Alignment.Center
    ) {
      //1284总宽度  计划宽度：209    17宽 38长     106     17/53  38/53
      Box(
        modifier = Modifier
          .width((width * 0.16f).dp)
          .aspectRatio(1f)
          .clip(RoundedCornerShape(10.dp))
          .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
      ) {
        Canvas(
          modifier = Modifier
            .fillMaxWidth(0.50f)
            .aspectRatio(1f) // 横纵比
        ) {
          val radius = size.width / 2 // 绘制区域的半径
          //圆弧形的矩形 长度
          val drawWidth = radius / 2 // 圆弧矩形的长度 半径的一半
          //圆弧形的矩形 宽度
          val strokeWidth = radius / 4 // 圆弧矩形的宽度 0.32*r
          if (LoadingViewModel.mTicker.value > 0) {
            for (index in 1..count) {
              // 中心点的坐标是 (radius, radius)，计算弧度 radians
              val radians = rotateAngle * index * PI / 180
              val startX = (radius + (radius - drawWidth) * cos(radians)).toFloat()
              val startY = (radius - (radius - drawWidth) * sin(radians)).toFloat()
              val endX = (radius + radius * cos(radians)).toFloat()
              val endY = (radius - radius * sin(radians)).toFloat()
              drawLine(
                color = LoadingViewModel.mColor[index - 1],
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                cap = StrokeCap.Round,
                strokeWidth = strokeWidth,
              )
            }
          }
        }
      }
    }
  }
}