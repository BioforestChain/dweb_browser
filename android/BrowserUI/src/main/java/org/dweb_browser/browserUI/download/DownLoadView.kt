package org.dweb_browser.browserUI.download

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import org.dweb_browser.browserUI.ui.view.DialogInfo
import org.dweb_browser.browserUI.ui.view.DialogType
import org.dweb_browser.browserUI.ui.view.DialogView
import org.dweb_browser.microservice.help.Mmid

/**
 * 显示下载进度框
 * @param path 提供下载地址
 * @param callbackState 回调下载结果信息
 *     DownLoadState: 表示下载状态，目前返回内容(DownLoadState.COMPLETED, DownLoadState.FAILURE)
 *     DialogInfo: 使用对话框形式返回下载结果的具体内容信息，可直接显示，当然也可以做相应调整后显示
 */
@Composable
fun DownloadDialogView(
  mmid: Mmid, path: String, callbackState: (DownLoadStatus, DialogInfo) -> Unit
) {
  val downLoadViewModel = DownLoadViewModel(mmid, path)
  downLoadViewModel.handleIntent(DownLoadIntent.DownLoad)
  DownloadDialogProgressView(
    downLoadViewModel = downLoadViewModel, callbackState = callbackState
  )
}

/**
 * 使用遮罩的形式显示下载进度
 * @param path 提供下载地址
 * @param callbackState 回调下载结果信息
 *     DownLoadState: 表示下载状态，目前返回内容(DownLoadState.COMPLETED, DownLoadState.FAILURE)
 *     DialogInfo: 使用对话框形式返回下载结果的具体内容信息，可直接显示，当然也可以做相应调整后显示
 */
@Composable
fun DownloadAppMaskView(
  mmid: Mmid,
  path: String,
  modifier: Modifier = Modifier,
  callbackState: (DownLoadStatus, DialogInfo) -> Unit,
) {
  val downLoadViewModel = DownLoadViewModel(mmid, path)
  downLoadViewModel.handleIntent(DownLoadIntent.DownLoad)
  DownloadAppProgressView(
    downLoadViewModel = downLoadViewModel,
    modifier = modifier,
    callbackState = callbackState,
  )
}

@Composable
fun DownloadAppMaskView(
  downLoadViewModel: DownLoadViewModel,
  modifier: Modifier = Modifier,
  callbackState: (DownLoadStatus, DialogInfo) -> Unit,
) {
  DownloadAppProgressView(
    downLoadViewModel = downLoadViewModel,
    modifier = modifier,
    callbackState = callbackState,
  )
}

@Composable
private fun DownloadAppProgressView(
  downLoadViewModel: DownLoadViewModel,
  modifier: Modifier,
  callbackState: (DownLoadStatus, DialogInfo) -> Unit,
) {
  val dialogInfo = DialogInfo(
    type = DialogType.PROGRESS,
    progress = downLoadViewModel.uiState.value.downLoadProgress.value
  )
  val show = remember {
    derivedStateOf { // 多个状态归类判断，只有出现变化后，才会刷新show值
      when (downLoadViewModel.uiState.value.downLoadState.value) {
        DownLoadStatus.DownLoading, DownLoadStatus.DownLoadComplete, DownLoadStatus.PAUSE -> true
        else -> false
      }
    }
  }
  val radius = remember { mutableFloatStateOf(0f) }
  var canvasSize by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(downLoadViewModel.uiState.value.downLoadState.value) { // 为了在状态变化后能够及时通知调用方刷新状态
    callbackState(
      downLoadViewModel.uiState.value.downLoadState.value,
      downLoadViewModel.uiState.value.dialogInfo
    )
  }
  if (show.value) {
    Box(modifier = modifier.clickable {
      downLoadViewModel.handleIntent(DownLoadIntent.DownLoadStatusChange)
    }) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        radius.value = size.minDimension / 2f
        canvasSize = size.maxDimension
        val topLeftOffset =
          Offset(size.width / 2 - radius.value, size.height / 2 - radius.value)
        val strokeWith = Stroke(12f)
        // 绘制一个白色的圆弧
        drawCircle(
          color = Color.White.copy(0.6f),
          style = strokeWith,
          radius = radius.value
        )
        // 绘制一个进度条
        drawArc(
          startAngle = -90f, // 0表示3点
          sweepAngle = 360 * dialogInfo.progress,
          color = Color.White.copy(0.6f), // 外围圆弧的颜色
          useCenter = true, // true 表示半径需要填充颜色
          size = size,
          topLeft = topLeftOffset,
          style = Fill // 圆弧宽度
        )
      }
    }
  } else {
    HideAppMaskView(
      radius = radius.value,
      canvasSize = canvasSize,
      modifier = modifier,
      callbackState = callbackState
    )
  }
}

@Composable
fun HideAppMaskView(
  radius: Float,
  canvasSize: Float,
  modifier: Modifier,
  callbackState: (DownLoadStatus, DialogInfo) -> Unit
) {
  var trigger by remember { mutableFloatStateOf(if (radius == 0f) 90f else radius) }
  var isFinished by remember { mutableStateOf(false) }

  val animatedRadius by animateFloatAsState(targetValue = trigger, animationSpec = tween(
    durationMillis = 200, easing = LinearEasing
  ), finishedListener = {
    isFinished = true
    callbackState(DownLoadStatus.INSTALLED, DialogInfo())
  }, label = ""
  )

  DisposableEffect(Unit) {
    trigger = if (canvasSize == 0f) 190f else canvasSize
    onDispose {}
  }

  if (!isFinished) {
    Box(modifier = modifier) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        translate { // 绘制一个白色的圆弧
          drawCircle(
            color = Color.White.copy(0.6f),
            style = Fill,
            radius = animatedRadius
          )
        }
      }
    }
  }
}


@Composable
private fun DownloadDialogProgressView(
  downLoadViewModel: DownLoadViewModel, callbackState: (DownLoadStatus, DialogInfo) -> Unit
) {
  val dialogInfo = DialogInfo(
    type = DialogType.PROGRESS,
    progress = downLoadViewModel.uiState.value.downLoadProgress.value
  )
  val show = remember {
    derivedStateOf { // 多个状态归类判断，只有出现变化后，才会刷新show值
      when (downLoadViewModel.uiState.value.downLoadState.value) {
        DownLoadStatus.DownLoading, DownLoadStatus.DownLoadComplete, DownLoadStatus.PAUSE -> true
        else -> false
      }
    }
  }
  when (downLoadViewModel.uiState.value.downLoadState.value) { // 为了在状态变化后能够及时通知调用方刷新状态
    DownLoadStatus.INSTALLED, DownLoadStatus.FAIL -> {
      callbackState(
        downLoadViewModel.uiState.value.downLoadState.value,
        downLoadViewModel.uiState.value.dialogInfo
      )
    }
    else -> {}
  }
  if (show.value) {
    DialogView(dialogInfo = dialogInfo)
  }
}
