package org.dweb_browser.browser.scan

import android.graphics.BitmapFactory
import android.util.Log
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.dweb_browser.helper.PureRect
import java.util.concurrent.Executors

@Composable
actual fun CameraPreviewRender(
  openAlarmResult: (ImageBitmap) -> Unit,
  onBarcodeDetected: (QRCodeDecoderResult) -> Unit,
  maskView: @Composable (FlashLightSwitch, OpenAlbum) -> Unit,
  onCancel: (String) -> Unit,
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
                PureRect(
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
      maskView({ torch -> item.cameraControl.enableTorch(torch); true },
        { launchPhoto.launch("image/*") })
    }
  }

  DisposableEffect(cameraProvider) {
    onDispose {
      cameraProvider?.unbindAll()
    }
  }
}