package org.dweb_browser.browser.common.barcode

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.dweb_browser.browser.R
import org.dweb_browser.browser.util.isUrl
import org.dweb_browser.browser.util.regexDeepLink
import org.dweb_browser.core.module.getAppContext
import java.util.concurrent.Executors
import kotlin.math.abs

@Composable
actual fun CameraPreviewView(
  openAlarmResult: (ImageBitmap) -> Unit,
  onBarcodeDetected: (QRCodeDecoderResult) -> Unit,
  maskView: @Composable (FlashLightSwitch, OpenAlbum) -> Unit
) {
  var cameraProvider: ProcessCameraProvider? = null
  val context = LocalContext.current
  val camera: MutableState<Camera?> = remember { mutableStateOf(null) }
  val launchPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    // TODO 将这个Bitmap解码
    uri?.let {
      val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
      openAlarmResult(bitmap.asImageBitmap())
    }
  }

  Surface(
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
          val listRect: MutableList<QRCodeDecoderResult.QRCode> = mutableListOf()
          barcodes.forEach { barcode ->
            listRect.add(
              QRCodeDecoderResult.QRCode(
                org.dweb_browser.helper.Rect(
                  x = barcode.boundingBox?.centerX()?.toFloat() ?: 0f,
                  y = barcode.boundingBox?.centerY()?.toFloat() ?: 0f
                ), barcode.displayValue
              )
            )
          }
          val qrCodeScanResult = QRCodeDecoderResult(
            preBitmap = previewView.bitmap?.asImageBitmap(),
            lastBitmap = bitmap?.asImageBitmap(),
            listQRCode = listRect
          )
          onBarcodeDetected(qrCodeScanResult)
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
    camera.value?.let { item ->
      maskView(
        { torch -> item.cameraControl.enableTorch(torch); true },
        { launchPhoto.launch("image/*") }
      )
    }
  }

  DisposableEffect(cameraProvider) {
    onDispose {
      cameraProvider?.unbindAll()
    }
  }
}

actual fun beepAudio() {
  (getAppContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager)
    .playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f)
}

actual fun decoderImage(
  imageBitmap: ImageBitmap, onSuccess: (QRCodeDecoderResult) -> Unit, onFailure: (Exception) -> Unit
) {
  val inputImage =
    InputImage.fromBitmap(imageBitmap.asAndroidBitmap(), android.view.Surface.ROTATION_0)
  BarcodeScanning.getClient(
    BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
  ).process(inputImage).addOnSuccessListener { result ->
    if (result.isEmpty()) {
      onFailure(Exception("No Found QR Code"))
    } else {
      val listRect: MutableList<QRCodeDecoderResult.QRCode> = mutableListOf()
      result.forEach { barcode ->
        listRect.add(
          QRCodeDecoderResult.QRCode(
            org.dweb_browser.helper.Rect(
              x = barcode.boundingBox?.centerX()?.toFloat() ?: 0f,
              y = barcode.boundingBox?.centerY()?.toFloat() ?: 0f
            ), null
          )
        )
      }
      val imageBitmap = inputImage.bitmapInternal?.asImageBitmap()
      val qrCodeDecoderResult = QRCodeDecoderResult(imageBitmap, imageBitmap, listRect)
      onSuccess(qrCodeDecoderResult)
    }
  }.addOnFailureListener { e -> onFailure(e) }
}


actual fun transformPoint(
  x: Int, y: Int, srcWidth: Int, srcHeight: Int, destWidth: Int, destHeight: Int, isFit: Boolean
): QRCodeDecoderResult.Point {
  val widthRatio = destWidth * 1.0f / srcWidth
  val heightRatio = destHeight * 1.0f / srcHeight
  return if (isFit) { //宽或高自适应铺满
    val ratio = widthRatio.coerceAtMost(heightRatio)
    val left = abs(srcWidth * ratio - destWidth) / 2
    val top = abs(srcHeight * ratio - destHeight) / 2
    QRCodeDecoderResult.Point(x * ratio + left, y * ratio + top)
  } else { //填充铺满（可能会出现裁剪）
    val ratio = widthRatio.coerceAtLeast(heightRatio)
    val left = abs(srcWidth * ratio - destWidth) / 2
    val top = abs(srcHeight * ratio - destHeight) / 2
    QRCodeDecoderResult.Point(x * ratio - left, y * ratio - top)
  }
}

actual fun openDeepLink(data: String) {
  val context = getAppContext()
  val deepLink = if (data.isUrl()) {
    "dweb://openinbrowser?url=$data"
  } else data

  deepLink.regexDeepLink()?.let { dwebLink ->
    context.startActivity(Intent().also {
      it.`package` = context.packageName
      it.action = Intent.ACTION_VIEW
      it.data = Uri.parse(dwebLink)
      it.addCategory("android.intent.category.BROWSABLE")
      it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
  } ?: Toast.makeText(
    context,
    context.getString(R.string.shortcut_toast) + data,
    Toast.LENGTH_SHORT
  ).show()
}