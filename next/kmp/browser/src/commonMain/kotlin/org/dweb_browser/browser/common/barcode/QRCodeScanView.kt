package org.dweb_browser.browser.common.barcode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.noLocalProvidedFor
import org.dweb_browser.sys.window.render.NativeBackHandler

val LocalQRCodeModel = compositionLocalOf<QRCodeScanModel> {
  noLocalProvidedFor("LocalQRCodeState")
}

@Composable
fun rememberQRCodeModel(): QRCodeScanModel {
  return remember { QRCodeScanModel() }
}

@Composable
fun QRCodeScanView(
  qrCodeScanModel: QRCodeScanModel,
  modifier: Modifier = Modifier,
  enableBeep: Boolean = true, // 扫码成功后是否打开提示音
  //flashLight: Boolean = false, // 闪关灯默认状态
  onSuccess: (String) -> Unit, // 成功返回扫码结果
  onCancel: (String) -> Unit, // 失败返回失败原因
) {
  DisposableEffect(qrCodeScanModel) {
    val off = qrCodeScanModel.onStateChange {
      if (it != QRCodeState.Scanning || qrCodeScanModel.checkPermission()) {
        qrCodeScanModel.state.value = it
      }
    }
    onDispose { off() }
  }
  if (qrCodeScanModel.state.value == QRCodeState.Hide) return // 隐藏状态不显示
  val scope = rememberCoroutineScope()
  NativeBackHandler {
    scope.launch {
      when (qrCodeScanModel.state.value) {
        QRCodeState.MultiSelect -> qrCodeScanModel.stateChange.emit(QRCodeState.Scanning)
        else -> {
          qrCodeScanModel.stateChange.emit(QRCodeState.Hide); onCancel("")
        }
      }
    }
  }
  AnimatedContent(
    targetState = qrCodeScanModel.state.value.type, label = "",
    transitionSpec = {
      if (targetState > initialState) {
        // 数字变大时，进入的界面从右向左变深划入，退出的界面从右向左变浅划出
        (slideInHorizontally { fullWidth -> fullWidth } + fadeIn()).togetherWith(
          slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut())
      } else {
        // 数字变小时，进入的数字从左向右变深划入，退出的数字从左向右变浅划出
        (slideInHorizontally { fullWidth -> -fullWidth } + fadeIn()).togetherWith(
          slideOutHorizontally { fullWidth -> fullWidth } + fadeOut())
      }
    }) { state ->
    when (state) {
      QRCodeState.Scanning.type -> {
        CameraPreviewView(
          openAlarmResult = { imageBitmap ->
            scope.launch {
              qrCodeScanModel.imageBitmap = imageBitmap
              qrCodeScanModel.stateChange.emit(QRCodeState.AnalyzePhoto)
            }
          },
          onBarcodeDetected = { qrCodeDecoderResult ->
            if (enableBeep) beepAudio()
            qrCodeScanModel.qrCodeDecoderResult = qrCodeDecoderResult
            scope.launch { qrCodeScanModel.stateChange.emit(QRCodeState.MultiSelect) }
          },
          maskView = { flashLightSwitch, openAlbum ->
            DefaultScanningView(flashLightSwitch = flashLightSwitch, openAlbum = openAlbum) {
              onCancel("")
            }
          }
        )
      }

      QRCodeState.AnalyzePhoto.type -> {
        AnalyzeImageView(
          onBackHandler = {
            scope.launch { qrCodeScanModel.stateChange.emit(QRCodeState.Scanning) }
          },
          onBarcodeDetected = { qrCodeDecoderResult ->
            if (enableBeep) beepAudio()
            qrCodeScanModel.qrCodeDecoderResult = qrCodeDecoderResult
            scope.launch { qrCodeScanModel.stateChange.emit(QRCodeState.MultiSelect) }
          }
        )
      }

      QRCodeState.MultiSelect.type -> {
        DefaultScanResultView(
          onClose = { scope.launch { qrCodeScanModel.stateChange.emit(QRCodeState.Scanning) } },
          onDataCallback = { onSuccess(it) }
        )
      }

      else -> {}
    }
  }
}

@Composable
private fun DefaultScanningView(
  flashLightSwitch: FlashLightSwitch, openAlbum: OpenAlbum, onClose: () -> Unit
) {

  Box(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
  ) {
    ScannerLine() // 添加扫描线
    CloseIcon { onClose() } // 关闭按钮
    FlashlightIcon(flashLightSwitch)
    AlbumButton(openAlbum)
  }
}


@Composable
private fun ScannerLine() {
  var linePosition by remember { mutableStateOf(0f) }
  val infiniteTransition = rememberInfiniteTransition(label = "")
  val animatedLinePosition by infiniteTransition.animateFloat(
    initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3000), repeatMode = RepeatMode.Restart
    ), label = ""
  )
  DisposableEffect(animatedLinePosition) {
    linePosition = animatedLinePosition
    onDispose { }
  }
  Box(modifier = Modifier.fillMaxSize()) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val canvasWidth = size.width
      val canvasHeight = size.height
      val lineWidth = canvasWidth * 0.8f
      val lineHeight = canvasHeight * 0.5f
      val lineStart = canvasWidth * 0.1f
      val lineTop = canvasHeight * 0.2f

      drawOval(
        color = Color(0xFF1FB3E2),
        topLeft = Offset(lineStart, lineTop + lineHeight * linePosition),
        size = Size(lineWidth, 3.dp.toPx()),
      )
    }
  }
}

@Composable
private fun BoxScope.AlbumButton(openAlbum: OpenAlbum) {
  Column(
    modifier = Modifier
      .padding(16.dp)
      .size(54.dp)
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.onBackground.copy(0.5f))
      .clickableWithNoEffect { openAlbum() }
      .align(Alignment.BottomEnd),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = Icons.Default.Photo,
      contentDescription = "Photo",
      tint = MaterialTheme.colorScheme.background,
      modifier = Modifier.size(22.dp)
    )
    Text(text = "相册", color = MaterialTheme.colorScheme.background, fontSize = 12.sp)
  }
}

@Composable
private fun BoxScope.FlashlightIcon(flashLightSwitch: FlashLightSwitch) {
  var lightState by remember { mutableStateOf(false) }
  val imageVector = if (lightState) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff
  var animationColor by remember { mutableStateOf(Color(0xFFFFFFFF)) }
  val animation = rememberInfiniteTransition(label = "")
  val animateColor by animation.animateColor(
    initialValue = Color(0xFFFFFFFF),
    targetValue = Color(0xFF000000),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2000), repeatMode = RepeatMode.Reverse
    ),
    label = ""
  )

  DisposableEffect(animateColor) {
    animationColor = animateColor
    onDispose { }
  }
  Box(modifier = Modifier
    .align(Alignment.BottomCenter)
    .padding(16.dp)
    .size(46.dp)
    .clip(CircleShape)
    .clickable {
      if (flashLightSwitch(!lightState)) {
        lightState = !lightState
      }
    }
  ) {
    Icon(
      imageVector = imageVector,
      contentDescription = "FlashLight",
      tint = animationColor,
      modifier = Modifier
        .align(Alignment.Center)
        .size(36.dp)
    )
  }
}

@Composable
private fun BoxScope.CloseIcon(onClick: () -> Unit) {
  Icon(imageVector = Icons.Default.AddCircle,
    contentDescription = "Close",
    tint = MaterialTheme.colorScheme.background,
    modifier = Modifier
      .padding(16.dp)
      .size(32.dp)
      .clickable { onClick() }
      .rotate(45f)
      .align(Alignment.TopStart))
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeImageView(
  onBackHandler: () -> Unit, onBarcodeDetected: (QRCodeDecoderResult) -> Unit
) {
  val scope = rememberCoroutineScope()
  val qrCodeScanModel = LocalQRCodeModel.current
  val imageBitmap = qrCodeScanModel.imageBitmap ?: run {  // 如果为空，直接返回
    scope.launch {
      qrCodeScanModel.stateChange.emit(QRCodeState.Scanning)
    }
    return
  }
  var showAlert by remember { mutableStateOf(true) }
  var alertMsg by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(imageBitmap) {
    delay(200)
    decoderImage(
      imageBitmap = imageBitmap,
      onSuccess = { onBarcodeDetected(it) },
      onFailure = { alertMsg = "analyze fail" }
    )
  }
  Box(modifier = Modifier
    .fillMaxSize()
    .background(Color.Black)
    .clickable(false) {}) {
    Image(
      bitmap = imageBitmap,
      contentDescription = "Photo",
      alignment = Alignment.Center,
      contentScale = ContentScale.Fit,
      modifier = Modifier.align(Alignment.Center)
    )

    alertMsg?.let { msg ->
      if (showAlert) {
        AlertDialog(
          onDismissRequest = { showAlert = false },
          modifier = Modifier
            .clip(AlertDialogDefaults.shape)
            .background(AlertDialogDefaults.containerColor),
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = msg, modifier = Modifier.padding(vertical = 20.dp))
            Text(
              text = "确认",
              modifier = Modifier
                .clickableWithNoEffect { showAlert = false; onBackHandler() }
                .padding(20.dp),
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    } ?: run {
      Box(
        modifier = Modifier
          .size(120.dp)
          .background(Color.Black.copy(0.5f))
          .align(Alignment.Center),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = Color.White)
          Spacer(modifier = Modifier.height(10.dp))
          Text(text = "正在识别...", color = Color.White)
        }
      }
    }
  }
}

@Composable
private fun DefaultScanResultView(onClose: () -> Unit, onDataCallback: (String) -> Unit) {
  val qrCodeScanModel = LocalQRCodeModel.current
  val scope = rememberCoroutineScope()
  val pointScale = remember { mutableFloatStateOf(1f) }
  val infiniteTransition = rememberInfiniteTransition(label = "")
  val animatedLinePosition by infiniteTransition.animateFloat(
    initialValue = 1f, targetValue = 0.8f, animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1000), repeatMode = RepeatMode.Reverse
    ), label = ""
  )
  DisposableEffect(animatedLinePosition) {
    pointScale.value = animatedLinePosition
    onDispose { }
  }

  LaunchedEffect(Unit) { // 判断是否只有一个结果，如果只有一个，等待500ms后，直接跳转
    qrCodeScanModel.qrCodeDecoderResult?.listQRCode?.let { listQRCode ->
      if (listQRCode.size == 1) {
        delay(500)
        val data = try {
          listQRCode.first().displayName ?: "data is null"
        } catch (e: Exception) {
          "data get fail -> ${e.message}"
        }
        onDataCallback(data)
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {

    qrCodeScanModel.qrCodeDecoderResult?.preBitmap?.let { imageBitmap ->
      Image(
        bitmap = imageBitmap,
        contentDescription = "Scan",
        modifier = Modifier.align(Alignment.Center),
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center
      )
    } ?: run {// 没有图片信息，不要显示选择
      scope.launch {
        qrCodeScanModel.stateChange.emit(QRCodeState.Scanning)
      }
      return
    }

    val pointList = arrayListOf<QRCodeDecoderResult.Point>()
    val barcodes = qrCodeScanModel.qrCodeDecoderResult?.listQRCode ?: emptyList()
    val bitmapPair =
      qrCodeScanModel.qrCodeDecoderResult?.lastBitmap?.let { Pair(it.width, it.height) } ?: Pair(
        0,
        0
      )

    Canvas(modifier = Modifier
      .matchParentSize()
      .pointerInput(Unit) {
        detectTapGestures { offset ->
          pointList.forEachIndexed { index, point ->
            if (offset.x >= point.x - 56f && offset.x <= point.x + 56f && offset.y >= point.y - 56f && offset.y <= point.y + 56f) {
              val data = try {
                barcodes[index].displayName ?: "data is null"
              } catch (e: Exception) {
                "data get fail -> ${e.message}"
              }
              onDataCallback(data)
              return@forEachIndexed
            }
          }
        }
      }) {
      val showBitmap = barcodes.size > 1
      barcodes.forEach { barcode ->
        val point = transformPoint(
          barcode.rect.x.toInt(),
          barcode.rect.y.toInt(),
          bitmapPair.first,
          bitmapPair.second,
          size.width.toInt(),
          size.height.toInt()
        )
        pointList.add(point) // 为了响应点击操作

        drawerPoint(point = point, scale = pointScale, showPic = showBitmap)
      }
    }
  }
  Box(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
  ) {
    CloseIcon { onClose() }
  }
}

private fun DrawScope.drawerPoint(
  point: QRCodeDecoderResult.Point, scale: MutableState<Float>, showPic: Boolean
) {
  val center = Offset(point.x, point.y)
  val radius = 56f * scale.value
  drawCircle(
    style = Fill,
    color = Color(0xFF1FB3E2),
    radius = radius,
    center = center,
  )
  drawCircle(
    style = Stroke(width = 6f),
    color = Color(0xFFFFFFFF),
    radius = radius,
    center = center,
  )
  if (showPic) { // 手动绘制箭头 TODO 使用path是为了转角连贯，如果三个都用drawLine会导致转角是断裂的
    val minus = radius / 2
    val path = Path()
    path.moveTo(center.x, center.y - minus)
    path.lineTo(center.x + minus, center.y)
    path.lineTo(center.x, center.y + minus)
    path.lineTo(center.x + minus, center.y)
    path.close()
    drawPath(path = path, color = Color(0xFFFFFFFF), style = Stroke(width = 3.dp.toPx()))
    drawLine(
      color = Color(0xFFFFFFFF),
      start = Offset(center.x - minus, center.y),
      end = Offset(center.x + minus, center.y),
      strokeWidth = 3.dp.toPx()
    )
  }
}