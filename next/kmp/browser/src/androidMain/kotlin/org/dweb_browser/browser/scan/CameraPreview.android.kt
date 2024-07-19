package org.dweb_browser.browser.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.globalMainScope
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors


/**这里是android相机的预览画到Compose上，并且输出扫描到的一贞一贞图片*/
@OptIn(ExperimentalGetImage::class)
@Composable
actual fun CameraPreviewRender(
  modifier: Modifier, controller: SmartScanController
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val imagePickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      // TODO 将这个Bitmap解码
      uri?.let {
        val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
        controller.imageCaptureFlow.tryEmit(bitmap)
      }
    }
  // 创建相机控制器
  val cameraController = remember {
    CameraControllerImpl(controller, imagePickerLauncher).also { c ->
      controller.cameraController?.stop()
      controller.cameraController = c // 赋值
    }
  }
  val previewView = remember { PreviewView(context) }
  DisposableEffect(Unit) {
    cameraController.initializeCamera(context, lifecycleOwner, previewView)
    onDispose {
      cameraController.stop()
    }
  }
  Box(Modifier.fillMaxSize()) {
    AndroidView(
      {
        previewView
      },
      modifier = Modifier.fillMaxSize()
    )
  }
}

class CameraControllerImpl(
  private val controller: SmartScanController,
  private val imagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>
) : CameraController {
  // 相机控制器
  private var cameraProvider = mutableStateOf<ProcessCameraProvider?>(null)

  // 当前相机实例
  private var camera: Camera? = null

  // 摄像头选择
  private val cameraSelector =
    CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

  // 初始化相机
  fun initializeCamera(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
      cameraProvider.value = cameraProviderFuture.get()
      bindPreview(cameraProvider.value!!, lifecycleOwner, previewView)
    }, ContextCompat.getMainExecutor(context))
  }

  @OptIn(TransformExperimental::class)
  private fun bindPreview(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView
  ) {
    val preview = Preview.Builder().build()
    val imageAnalyzer = ImageAnalysis.Builder()
      .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST) // 仅将最新图像传送到分析器，并在图像到达时丢弃它们。
//      .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) // 设置输出格式
      .build().also {
        it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
          globalMainScope.launch {
            val matrix = getCoordinateTransform(imageProxy, previewView)
            controller.imageCaptureFlow.tryEmit(Pair(imageProxy, matrix))
          }
        }
      }
    cameraProvider.unbindAll()
    try {
      camera = cameraProvider.bindToLifecycle(
        lifecycleOwner, cameraSelector, preview, imageAnalyzer
      )
      preview.surfaceProvider = previewView.surfaceProvider
    } catch (exc: Exception) {
      WARNING(exc.message)
    }
  }


  /**打开手电筒*/
  override fun toggleTorch() {
    camera?.let {
      val currentTorchState = it.cameraInfo.torchState.value
      if (currentTorchState == TorchState.ON) {
        it.cameraControl.enableTorch(false)
      } else {
        it.cameraControl.enableTorch(true)
      }
    }
  }

  /**打开相册获取图片识别*/
  override fun openAlbum() {
    imagePickerLauncher.launch("image/*")
  }

  override fun stop() {
    cameraProvider.value?.unbindAll()
  }

  /**转换*/
  @OptIn(TransformExperimental::class)
  private fun getCoordinateTransform(
    imageProxy: ImageProxy,
    previewView: PreviewView
  ): CoordinateTransform {
    // imageProxy ImageAnalysis的输出。
    val source = ImageProxyTransformFactory().getOutputTransform(imageProxy)
    val target = previewView.outputTransform

    // 构建从ImageAnalysis到PreviewView的转换
    val coordinateTransform = CoordinateTransform(source, target!!)
    return coordinateTransform
  }
}


@OptIn(ExperimentalGetImage::class)
fun ImageProxy.toByteArray(previewView: PreviewView): ByteArray {
  val planes = image!!.planes
  val buffer: ByteBuffer = planes[0].buffer
  val pixelStride: Int = planes[0].pixelStride
  val rowStride: Int = planes[0].rowStride
  val rowPadding = rowStride - pixelStride * image!!.width
  val bitmap = Bitmap.createBitmap(
    image!!.width + rowPadding / pixelStride,
    image!!.height, Bitmap.Config.ARGB_8888
  )
  bitmap.copyPixelsFromBuffer(buffer)
  val stream = ByteArrayOutputStream()
  bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
  return stream.toByteArray()
}