package org.dweb_browser.browserUI.ui.loading

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import org.dweb_browser.browserUI.bookmark.clickableWithNoEffect
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LoadingView(
  showLoading: MutableState<Boolean>,
  viewModel: LoadingViewModel = LoadingViewModel(),
) {
  val whiteBackground = !isSystemInDarkTheme()
  if (showLoading.value) {
    LaunchedEffect(Unit) {
      viewModel.setBackground(whiteBackground)
      viewModel.startTimer(System.currentTimeMillis())
    }
    DisposableEffect(viewModel) {
      // When the effect leaves the Composition, remove the observer
      onDispose {
        viewModel.timerDestroy()
      }
    }
    BackHandler { showLoading.value = false }
    val width = LocalConfiguration.current.screenWidthDp
    val count = 8
    val rotateAngle = (360 / count).toDouble()
    Box(modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .clickableWithNoEffect {  },
      contentAlignment = Alignment.Center
    ) {
      //1284总宽度  计划宽度：209    17宽 38长     106     17/53  38/53
      Box(
        modifier = Modifier
          .width((width * 0.16f).dp)
          .aspectRatio(1f)
          .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
      ) {
        Canvas(
          modifier = Modifier
            .fillMaxWidth(0.50f)
            .aspectRatio(1f)
        ) {
          val r = size.width / 2
          //圆弧形的矩形 长度
          val drawWidth = 0.50 * r
          //圆弧形的矩形 宽度
          val strokeWidth = 0.32 * r
          if (viewModel.mTicker.value > 0) {
            for (index in 1..count) {
              val startX =
                (r + (r - drawWidth) * cos(Math.toRadians(rotateAngle * index))).toFloat()
              val startY =
                (r - (r - drawWidth) * sin(Math.toRadians(rotateAngle * index))).toFloat()
              val endX = (r + r * cos(Math.toRadians(rotateAngle * index))).toFloat()
              val endY = (r - r * sin(Math.toRadians(rotateAngle * index))).toFloat()
              drawLine(
                color = viewModel.mColor[index - 1],
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                cap = StrokeCap.Round,
                strokeWidth = strokeWidth.toFloat(),
              )
            }
          }
        }
      }
    }
  }
}