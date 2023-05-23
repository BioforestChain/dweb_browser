package info.bagen.dwebbrowser.ui.qrcode

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.Executors
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.ui.view.PermissionSingleView
import kotlinx.coroutines.delay

internal const val PERMISSION_CAMERA = android.Manifest.permission.CAMERA
internal const val PERMISSION_WRITE = android.Manifest.permission.WRITE_EXTERNAL_STORAGE

data class QRCodeScanState(
  val state: MutableState<QRCodeState> = mutableStateOf(QRCodeState.Hide),
  val analyzeResult: AnalyzeResult = AnalyzeResult(),
) {
  enum class QRCodeState(val type: Int) {
    Hide(0), Scanning(1), AnalyzePhoto(2), Completed(3);
  }

  class AnalyzeResult {
    var bitmap: Bitmap? = null
    var previewBitmap: Bitmap? = null
    var barcodes: List<Barcode>? = null
  }

  var uri: Uri? = null

  fun updateAnalyzeResult(bitmap: Bitmap?, previewBitmap: Bitmap?, barcodes: List<Barcode>) {
    analyzeResult.bitmap = bitmap
    analyzeResult.previewBitmap = previewBitmap
    analyzeResult.barcodes = barcodes
  }

  fun show() {
    state.value = QRCodeState.Scanning
  }

  fun hide() {
    state.value = QRCodeState.Hide
  }

  val isHidden get() = state.value == QRCodeState.Hide
}

@Composable
fun rememberQRCodeScanState(): QRCodeScanState {
  return remember { QRCodeScanState() }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QRCodeScanView(
  qrCodeScanState: QRCodeScanState = rememberQRCodeScanState(),
  @SuppressLint("ModifierParameter") modifier: Modifier = Modifier.fillMaxSize(),
  onDataCallback: (String) -> Unit, // 返回数据
  enableBeep: Boolean = true, // 扫码成功后是否打开提示音
  scanningContent: @Composable (Camera) -> Unit = {
    DefaultScanningView(camera = it, onSelect = { uri ->
      qrCodeScanState.uri = uri
      qrCodeScanState.state.value = QRCodeScanState.QRCodeState.AnalyzePhoto
    }, onClose = { qrCodeScanState.state.value = QRCodeScanState.QRCodeState.Hide })
  },
  scanResultContent: @Composable (QRCodeScanState.AnalyzeResult) -> Unit = { analyzeResult ->
    DefaultScanResultView(analyzeResult = analyzeResult,
      onClose = { qrCodeScanState.state.value = QRCodeScanState.QRCodeState.Scanning }) { data ->
      qrCodeScanState.state.value = QRCodeScanState.QRCodeState.Hide
      onDataCallback(data)
    }
  },
) {
  BackHandler {
    val type = when (qrCodeScanState.state.value) {
      QRCodeScanState.QRCodeState.Completed -> QRCodeScanState.QRCodeState.Scanning
      else -> QRCodeScanState.QRCodeState.Hide
    }
    qrCodeScanState.state.value = type
  }
  val launchCamera = rememberPermissionState(permission = PERMISSION_CAMERA)
  val context = LocalContext.current

  AnimatedContent(targetState = qrCodeScanState.state.value.type, label = "", transitionSpec = {
    if (targetState > initialState) {
      // 数字变大时，进入的界面从右向左变深划入，退出的界面从右向左变浅划出
      slideInHorizontally { fullWidth -> fullWidth } + fadeIn() with slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut()
    } else {
      // 数字变小时，进入的数字从左向右变深划入，退出的数字从左向右变浅划出
      slideInHorizontally { fullWidth -> -fullWidth } + fadeIn() with slideOutHorizontally { fullWidth -> fullWidth } + fadeOut()
    }
  }) { state ->
    when (state) {
      QRCodeScanState.QRCodeState.Scanning.type -> {
        PermissionSingleView(permissionState = launchCamera,
          onPermissionDenied = { qrCodeScanState.state.value = QRCodeScanState.QRCodeState.Hide }) {
          Box(modifier = modifier.background(Color.Black)) {
            CameraSurfaceView(onBarcodeDetected = { preview, bitmap, barcodes ->
              if (enableBeep) beepAudio(context)
              qrCodeScanState.updateAnalyzeResult(bitmap, preview, barcodes)
              qrCodeScanState.state.value = QRCodeScanState.QRCodeState.Completed
            }) { scanningContent(it) }
          }
        }
      }

      QRCodeScanState.QRCodeState.AnalyzePhoto.type -> {
        qrCodeScanState.uri?.let { uri ->
          Box(modifier = modifier.background(Color.Black)) {
            AnalyzePhotoView(uri = uri,
              onBackHandler = {
                qrCodeScanState.state.value = QRCodeScanState.QRCodeState.Scanning
              }) { preview, bitmap, barcodes ->
              if (enableBeep) beepAudio(context)
              qrCodeScanState.updateAnalyzeResult(bitmap, preview, barcodes)
              qrCodeScanState.state.value = QRCodeScanState.QRCodeState.Completed
            }
          }
        } ?: run {
          qrCodeScanState.state.value = QRCodeScanState.QRCodeState.Scanning
        }
      }

      QRCodeScanState.QRCodeState.Completed.type -> {
        Box(modifier = modifier.background(Color.Black)) {
          scanResultContent(qrCodeScanState.analyzeResult)
        }
      }

      else -> {}
    }
  }
}

@Composable
private fun CameraSurfaceView(
  onBarcodeDetected: (Bitmap?, Bitmap?, List<Barcode>) -> Unit,
  onContent: @Composable (Camera) -> Unit,
) {
  var cameraProvider: ProcessCameraProvider? = null
  val context = LocalContext.current
  val camera: MutableState<Camera?> = remember { mutableStateOf(null) }

  DisposableEffect(Surface(
    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.onBackground
  ) {
    AndroidView(factory = { ctx ->
      val previewView = PreviewView(ctx).also {
        it.scaleType = PreviewView.ScaleType.FILL_CENTER
      }
      val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
      cameraProviderFuture.addListener({
        cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
          it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalyzer =
          ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        val qrCodeAnalyser = QRCodeAnalyzer(onFailure = {}) { bitmap, barcodes ->
          imageAnalyzer.clearAnalyzer()
          onBarcodeDetected(previewView.bitmap, bitmap, barcodes)
        }
        imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), qrCodeAnalyser)

        val imageCapture = ImageCapture.Builder().build()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA //相机选择器
        try {
          cameraProvider!!.unbindAll() // Unbind use cases before rebinding
          // Bind use cases to camera
          camera.value = cameraProvider!!.bindToLifecycle(
            context as ComponentActivity, cameraSelector, preview, imageCapture, imageAnalyzer
          )
        } catch (exc: Exception) {
          Log.e("QRCodeScanView", "Use case binding failed", exc)
        }
      }, ContextCompat.getMainExecutor(context))
      previewView
    })
    camera.value?.let { onContent(it) }
  }) {
    onDispose {
      cameraProvider?.unbindAll()
    }
  }
}

@Composable
private fun DefaultScanningView(camera: Camera, onSelect: (Uri) -> Unit, onClose: () -> Unit) {
  val launchPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    // TODO 将这个Bitmap解码
    uri?.let { onSelect(uri) }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
  ) {
    ScannerLine() // 添加扫描线
    CloseIcon { onClose() } // 关闭按钮
    FlashlightIcon(camera)
    AlbumButton { launchPhoto.launch("image/*") }
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
private fun BoxScope.AlbumButton(onClick: () -> Unit) {
  Box(modifier = Modifier
    .padding(16.dp)
    .size(48.dp)
    .clip(CircleShape)
    .background(MaterialTheme.colorScheme.onBackground.copy(0.5f))
    .clickable { onClick() }
    .align(Alignment.BottomEnd)) {
    Column(
      modifier = Modifier.align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_photo),
        contentDescription = "Photo",
        tint = MaterialTheme.colorScheme.background,
        modifier = Modifier.size(22.dp)
      )
      Text(text = "相册", color = MaterialTheme.colorScheme.background, fontSize = 12.sp)
    }
  }
}

@Composable
private fun DefaultScanResultView(
  analyzeResult: QRCodeScanState.AnalyzeResult,
  onClose: () -> Unit,
  onDataCallback: (String) -> Unit
) {
  val pointScale = remember { mutableStateOf(1f) }
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

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {

    analyzeResult.previewBitmap?.let { bitmap ->
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Scan",
        modifier = Modifier.align(Alignment.Center),
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center
      )
    }/* ?: run {// 没有图片信息，不要显示选择
      state.state.value = QRCodeScanState.QRCodeState.Scanning
      return
    }*/
    val pointList = arrayListOf<Point>()
    val barcodes = analyzeResult.barcodes ?: emptyList()
    val bitmapPair = analyzeResult.bitmap?.let { Pair(it.width, it.height) } ?: Pair(0, 0)

    Canvas(modifier = Modifier
      .matchParentSize()
      .pointerInput(Unit) {
        detectTapGestures { offset ->
          pointList.forEachIndexed { index, point ->
            if (offset.x >= point.x - 56f && offset.x <= point.x + 56f && offset.y >= point.y - 56f && offset.y <= point.y + 56f) {
              val data = try {
                barcodes[index].displayValue ?: "data is null"
              } catch (e: java.lang.Exception) {
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
        val rect = barcode.boundingBox ?: android.graphics.Rect()
        val point = PointUtils.transform(
          rect.centerX(),
          rect.centerY(),
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
  point: Point, scale: MutableState<Float>, showPic: Boolean
) {
  val center = Offset(point.x.toFloat(), point.y.toFloat())
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

@Composable
private fun BoxScope.FlashlightIcon(camera: Camera) {
  var lightState by remember { mutableStateOf(false) }
  val iconDrawableId = if (lightState) R.drawable.ic_flashlight_on else R.drawable.ic_flashlight_off
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
    .clickable { camera.cameraControl.enableTorch(!lightState); lightState = !lightState }
  ) {
    Icon(
      bitmap = ImageBitmap.imageResource(iconDrawableId),
      contentDescription = "FlashLight",
      tint = animationColor,
      modifier = Modifier
        .align(Alignment.Center)
        .size(36.dp)
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyzePhotoView(
  uri: Uri, onBackHandler: () -> Unit, onBarcodeDetected: (Bitmap, Bitmap, List<Barcode>) -> Unit
) {
  BackHandler { onBackHandler() }
  val context = LocalContext.current
  val bitmap = remember { BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri)) }
  var showAlert by remember { mutableStateOf(true) }
  var alertMsg by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(uri) {
    delay(1000)
    BarcodeDecoder.process(BarcodeDecoder.fromBitmap(bitmap), onSuccess = {
      onBarcodeDetected(bitmap, bitmap, it)
    }, onFailure = {
      alertMsg = it.message
    }, format = Barcode.FORMAT_ALL_FORMATS
    )
  }
  Box(modifier = Modifier
    .fillMaxSize()
    .background(Color.Black)
    .clickable(false) {}) {
    Image(
      bitmap = bitmap.asImageBitmap(),
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
                .clickable(indication = null,
                  onClick = { showAlert = false; onBackHandler() },
                  interactionSource = remember { MutableInteractionSource() })
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