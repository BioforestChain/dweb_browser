package org.dweb_browser.browser.scan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.twotone.FlashlightOff
import androidx.compose.material.icons.twotone.FlashlightOn
import androidx.compose.material.icons.twotone.PhotoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min


internal class BarcodeResultDrawer(val index: Int, result: BarcodeResult) {
  var result by mutableStateOf(result)
  var visible by mutableStateOf(false)
  val bgAlphaAni = Animatable(0f)
}

/**渲染识别后的图片*/
@Composable
internal fun SmartScanController.RenderScanResultView(modifier: Modifier) {
  // 框出二维码框框的动画效果
  val infiniteTransition = rememberInfiniteTransition()
  val animatedOffset by infiniteTransition.animateFloat(
    initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(
      animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart
    )
  )
  val resultList by barcodeResultFlow.collectAsState()
  BoxWithConstraints(modifier = modifier.clipToBounds()) {
    // 画出框框
    Canvas(modifier = Modifier.fillMaxSize()) {
      for (result in resultList) {
        drawAnimatedBoundingBox(result, animatedOffset)
      }
    }
    // 画出识别到的内容
    val draws = remember { mutableStateMapOf<String, BarcodeResultDrawer>() }
    draws.values.forEach { it.visible = false }
    resultList.forEach {
      draws.getOrPut(it.data) { BarcodeResultDrawer(draws.size + 1, it) }.apply {
        result = it
        visible = true
      }
    }
    LaunchedEffect(draws.size) {
      scanningController.decodeHaptics()
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    for ((key, drawer) in draws) {
      key(key) {
        AnimatedVisibility(
          drawer.visible,
          enter = fadeIn(),
          exit = fadeOut(tween(durationMillis = 500, easing = LinearEasing))
        ) {
          Box(Modifier.fillMaxSize()) {// 需要一个外壳，否则会错误
            ScanButtonPreview(
              onClick = {
                scope.launch {
                  launch {
                    drawer.bgAlphaAni.snapTo(1f)
                    drawer.bgAlphaAni.animateTo(0f, tween(durationMillis = 1000))
                  }
                  listState.animateScrollToItem(draws.size - drawer.index)
                }
              },
              drawer = drawer,
            )
          }
        }
      }
    }
    /// 画出结果列表
    ChatScreenPreview(listState, draws.values.toList().sortedBy { -it.index })
  }
}

/**我是扫码显示内容的按钮，现在还有点丑*/
@Composable
internal fun SmartScanController.ScanButtonPreview(
  onClick: () -> Unit,
  drawer: BarcodeResultDrawer,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current.density
  var textIntSize by remember { mutableStateOf(IntSize.Zero) }
  val result = drawer.result
  val size by animateFloatAsState(
    (max(result.boundingBox.height, result.boundingBox.width) / density)
  )
  val offsetX by animateFloatAsState((result.boundingBox.x + result.boundingBox.width / 2) / density)
  val offsetY by animateFloatAsState((result.boundingBox.y + result.boundingBox.height / 2) / density)
  val fontSize = min(14f, (size / 2)).sp
  val colors = ButtonDefaults.filledTonalButtonColors().run {
    copy(
      contentColor = contentColor.copy(alpha = 0.9f),
      containerColor = containerColor.copy(alpha = 0.5f),
    )
  }
  val padding = (fontSize.value / 2).dp
  Box(
    modifier = modifier.sizeIn(
      maxWidth = (result.boundingBox.width * 0.8 / density).dp,
      maxHeight = (result.boundingBox.height * 0.8 / density).dp,
    ).aspectRatio(1f).offset(offsetX.dp, offsetY.dp).graphicsLayer {
      translationX = -textIntSize.width / 2f
      translationY = -textIntSize.height / 2f
    }.onGloballyPositioned { textIntSize = it.size }.clip(RoundedCornerShape(padding * 2))
      .background(colors.containerColor).clickable { onClick() },
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = "${drawer.index}",
      maxLines = 1,
      style = MaterialTheme.typography.bodySmall.copy(
        fontSize = fontSize, color = colors.contentColor, fontWeight = FontWeight.Bold
      ),
      modifier = Modifier.shadow(padding),
    )
  }
}

/**把二维码框出来，必须使用canvas才能画出偏移角度*/
fun DrawScope.drawAnimatedBoundingBox(barcode: BarcodeResult, animatedOffset: Float) {
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
    ), start = Offset(0f, 0f), end = Offset(size.width, size.height), tileMode = TileMode.Repeated
  )
  val pathMeasure = PathMeasure()
  pathMeasure.setPath(path, false)

  val length = pathMeasure.length
  val segmentLength = 100f  // 移动段的长度
  val offset = length * animatedOffset

  // 计算段的开始和结束位置
  val start = offset - segmentLength
  val dashPathEffect = PathEffect.dashPathEffect(
    floatArrayOf(segmentLength, length - segmentLength), if (start < 0) length + start else start
  )
  drawPath(
    path = path, brush = gradient, style = Stroke(
      width = 12f, pathEffect = dashPathEffect, cap = StrokeCap.Round,  // 圆角端点
      join = StrokeJoin.Round  // 圆角连接
    )
  )
}

/**扫码节目中的扫描线，灯光按钮等UI*/
@Composable
fun SmartScanController.DefaultScanningView(modifier: Modifier, showLight: Boolean = true) {
  Box(modifier = modifier) {
    ScannerLine() // 添加扫描线
    CloseIcon { onCancel("close") } // 关闭按钮
    // 内窥按钮
//    WarpButton(
//      alignment = Alignment.BottomStart,
//      openHandle = {
//        previewTypes = SmartModuleTypes.Endoscopic
//      }) {
//      Icon(
//        imageVector = Icons.Default.Fullscreen,
//        contentDescription = "Endoscopic",
//        tint = MaterialTheme.colorScheme.background,
//        modifier = Modifier.size(22.dp)
//      )
//      Text(
//        text = BrowserI18nResource.QRCode.photo_endoscopic(),
//        color = MaterialTheme.colorScheme.background,
//        fontSize = 12.sp
//      )
//    }

    Row(
      Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(8.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      var isTorchOpened by remember { mutableStateOf(false) }
      FilledTonalIconToggleButton(
        checked = isTorchOpened,
        onCheckedChange = {
          cameraController?.apply {
            toggleTorch()
            isTorchOpened = !isTorchOpened
          }
        },
        enabled = showLight,
        colors = IconButtonDefaults.filledTonalIconToggleButtonColors().run {
          remember(this) {
            IconToggleButtonColors(
              containerColor = containerColor.copy(0.5f),
              contentColor = contentColor,
              disabledContainerColor = disabledContainerColor.copy(0.5f),
              disabledContentColor = disabledContentColor,
              checkedContainerColor = checkedContainerColor.copy(0.5f),
              checkedContentColor = checkedContentColor,
            )
          }
        }
      ) {
        Icon(
          if (isTorchOpened) Icons.TwoTone.FlashlightOn else Icons.TwoTone.FlashlightOff,
          contentDescription = "Toggle Flashlight",
        )
      }

      FilledTonalIconButton(
        onClick = {
          cameraController?.openAlbum()
        },
        colors = IconButtonDefaults.filledTonalIconButtonColors().run {
          remember(this) {
            IconButtonColors(
              containerColor = containerColor.copy(0.5f),
              contentColor = contentColor,
              disabledContainerColor = disabledContainerColor.copy(0.5f),
              disabledContentColor = disabledContentColor,
            )
          }
        }
      ) {
        Icon(
          Icons.TwoTone.PhotoLibrary,
          contentDescription = "Pick Photo",
        )
      }
    }
  }
}


@Composable
fun ScannerLine() {
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
private fun BoxScope.CloseIcon(onClick: () -> Unit) {
  Icon(
    imageVector = Icons.Default.AddCircle,
    contentDescription = "Close",
    tint = MaterialTheme.colorScheme.background,
    modifier = Modifier.clickable { onClick() }.padding(16.dp).size(32.dp).rotate(45f)
      .align(Alignment.TopStart)
  )
}