package org.dweb_browser.browser.scan

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.div
import org.dweb_browser.sys.window.core.WindowContentRenderScope


@Composable
fun WindowContentRenderScope.RenderBarcodeScanning(
  modifier: Modifier,
  controller: SmartScanController
) {
  val viewScope = rememberCoroutineScope()
  val isOpenAlbum = mutableStateOf(false)

  // 视图切换,如果扫描到了二维码
  Box(modifier) {
    CameraPreviewRender(
      modifier = modifier,
      controller = controller
    )
    controller.DefaultScanningView(modifier)
    controller.RenderScanResultView(modifier)
  }
}

/**渲染识别后的图片*/
@Composable
internal fun SmartScanController.RenderScanResultView(modifier: Modifier) {
  val infiniteTransition = rememberInfiniteTransition()
  val animatedOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(4000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    )
  )

  Box(modifier = modifier) {
    val resultList by barcodeResultFlow.collectAsState()
    val density = LocalDensity.current.density
    Canvas(modifier = modifier.matchParentSize()) {
      for (result in resultList) {
        drawAnimatedBoundingBox(result, animatedOffset)
      }
    }
    for (result in resultList) {
      key(result.data) {
        var textSize by remember { mutableStateOf(Size.Zero) }
        val width = (result.boundingBox.width / density).toInt()
        val height = (result.boundingBox.height / density).toInt()
        FilledTonalButton(
          { onSuccess(result.data) },
          modifier = Modifier.width(width.dp).graphicsLayer {
            translationX = result.boundingBox.x + width - textSize.width
            translationY = result.boundingBox.y + height - textSize.height
          }.onGloballyPositioned { textSize = it.size.div(density) },
          colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White),
          border = BorderStroke(width = 0.5.dp, brush = SolidColor(Color.White)),
          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 3.dp)
        ) {
          Text(
            result.data,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

/**把二维码框出来，必须使用canvas才能画出偏移角度*/
private fun DrawScope.drawAnimatedBoundingBox(barcode: BarcodeResult, animatedOffset: Float) {
  val path = Path().apply {
    moveTo(barcode.topLeft.x, barcode.topLeft.y)
    lineTo(barcode.topRight.x, barcode.topRight.y)
    lineTo(barcode.bottomRight.x, barcode.bottomRight.y)
    lineTo(barcode.bottomLeft.x, barcode.bottomLeft.y)
    close()
  }
  val gradient = Brush.linearGradient(
    colors = listOf(
      Color(0xFFe91e63), // Pink
      Color(0xFF9c27b0), // Purple
      Color(0xFFab397d), // Light Purple
      Color(0xFF0899f9), // Light Blue
      Color(0xFF3f51b5), // Indigo
      Color(0xFFffeb3b), // Yellow
      Color(0xFF00bcd4), // Cyan
      Color(0xFF03a9f4), // Blue
      Color(0xFFffeb3b), // Yellow
      Color(0xFF2196f3), // Blue
      Color(0xFFf18842), // Orange
      Color(0xFF44cadc), // Light Cyan
      Color(0xFFff9800), // Orange
      Color(0xFFff5722), // Deep Orange
      Color(0xFFff9800), // Orange
      Color(0xFFffeb3b)  // Yellow
    ),
    start = Offset(0f, 0f),
    end = Offset(size.width, size.height),
    tileMode = TileMode.Repeated
  )
  val pathMeasure = PathMeasure()
  pathMeasure.setPath(path, false)

  val length = pathMeasure.length
  val segmentLength = 100f  // 移动段的长度
  val offset = length * animatedOffset

  // 计算段的开始和结束位置
  val start = offset - segmentLength
  val dashPathEffect = PathEffect.dashPathEffect(
    floatArrayOf(segmentLength, length - segmentLength),
    if (start < 0) length + start else start
  )
  drawPath(
    path = path,
    brush = gradient,
    style = Stroke(
      width = 12f,
      pathEffect = dashPathEffect,
      cap = StrokeCap.Round,  // 圆角端点
      join = StrokeJoin.Round  // 圆角连接
    )
  )
}

/**识别到空二维码视图*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenderEmptyResult(controller: SmartScanController) {
  var showAlert by remember { mutableStateOf(true) }
  if (showAlert) {
    BasicAlertDialog(
      onDismissRequest = { showAlert = false },
      modifier = Modifier.clip(AlertDialogDefaults.shape)
        .background(AlertDialogDefaults.containerColor)
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = BrowserI18nResource.QRCode.emptyResult.text,
          modifier = Modifier.padding(vertical = 20.dp)
        )
        Text(text = BrowserI18nResource.QRCode.confirm(),
          modifier = Modifier.clickableWithNoEffect {
            showAlert = false; controller.onCancel("close")
          }
            .padding(20.dp),
          color = MaterialTheme.colorScheme.primary)
      }
    }
  }
}

@Composable
private fun SmartScanController.DefaultScanningView(modifier: Modifier = Modifier) {
  Box(modifier = Modifier.fillMaxSize()) {
    ScannerLine() // 添加扫描线
    CloseIcon { onCancel("close") } // 关闭按钮
    FlashlightIcon {
      cameraController?.toggleTorch()
    }
//    AlbumButton {
//      cameraController?.openAlbum()
//    }
  }
}


@Composable
private fun ScannerLine() {
  var linePosition by remember { mutableFloatStateOf(0f) }
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
        color = Color.White.copy(alpha = 0.7f),
        topLeft = Offset(lineStart, lineTop + lineHeight * linePosition),
        size = Size(lineWidth, 3.dp.toPx()),
      )
    }
  }
}

@Composable
private fun BoxScope.AlbumButton(openAlbum: () -> Unit) {
  Column(
    modifier = Modifier.padding(16.dp).size(54.dp).clip(CircleShape)
      .background(MaterialTheme.colorScheme.onBackground.copy(0.5f))
      .clickableWithNoEffect {
        openAlbum()
      }.align(Alignment.BottomEnd),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = Icons.Default.Photo,
      contentDescription = "Photo",
      tint = MaterialTheme.colorScheme.background,
      modifier = Modifier.size(22.dp)
    )
    Text(
      text = BrowserI18nResource.QRCode.photo_album(),
      color = MaterialTheme.colorScheme.background,
      fontSize = 12.sp
    )
  }
}

/**画出相机等*/
@Composable
private fun BoxScope.FlashlightIcon(toggleTorch: () -> Unit) {
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
  Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).size(46.dp).clip(CircleShape)
    .clickable {
      lightState = !lightState
      toggleTorch()
    }) {
    Icon(
      imageVector = imageVector,
      contentDescription = "FlashLight",
      tint = animationColor,
      modifier = Modifier.align(Alignment.Center).size(36.dp)
    )
  }
}

@Composable
private fun BoxScope.CloseIcon(onClick: () -> Unit) {
  Icon(
    imageVector = Icons.Default.AddCircle,
    contentDescription = "Close",
    tint = MaterialTheme.colorScheme.background,
    modifier = Modifier.clickable { onClick() }.padding(16.dp).size(32.dp).rotate(45f)
      .align(Alignment.TopStart)
  )
}