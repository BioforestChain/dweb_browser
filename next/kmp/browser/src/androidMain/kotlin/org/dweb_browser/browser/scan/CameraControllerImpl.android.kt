package org.dweb_browser.browser.scan

import android.content.Context
import android.net.Uri
import android.util.Size
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.globalDefaultScope
import java.util.concurrent.Executors

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

  private val _surfaceRequests = MutableStateFlow<SurfaceRequest?>(null)
  val surfaceRequests: StateFlow<SurfaceRequest?>
    get() = _surfaceRequests.asStateFlow()

  // 初始化相机
  fun initializeCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraBoundSize: Size,
//    surfaceRequest: SurfaceRequest,
//    coordinateTransformer: CoordinateTransformer
  ) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
      cameraProvider.value = cameraProviderFuture.get()
      bindPreview(cameraProvider.value!!, lifecycleOwner, context, cameraBoundSize)
    }, ContextCompat.getMainExecutor(context))
  }

  private fun bindPreview(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    context: Context,
    cameraBoundSize: Size,
//    surfaceRequest: SurfaceRequest,
//    coordinateTransformer: CoordinateTransformer
  ) {
    val preview = Preview.Builder().build()

    val analysisBuilder = ImageAnalysis.Builder()
      .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // 仅将最新图像传送到分析器，并在图像到达时丢弃它们。
      .setResolutionSelector(
        ResolutionSelector.Builder()
          .setResolutionStrategy(
            surfaceRequests.value?.let {
              ResolutionStrategy(
                it.resolution,
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
              )
            } ?: ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
          ).build()
      )

    // 确保在 PreviewView 的布局完成之后再设置
    val imageAnalyzer = analysisBuilder
      .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) // 设置输出格式
      .build().also { analysis ->
        analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
          globalDefaultScope.launch {
            controller.decodeQrCode {
              recognize(imageProxy)
            }
            imageProxy.close()
          }
        }
      }
    cameraProvider.unbindAll()
    try {
      camera = cameraProvider.bindToLifecycle(
        lifecycleOwner, cameraSelector, preview, imageAnalyzer
      )
      // 进行视图操作
      preview.setSurfaceProvider { surfaceRequest ->
        _surfaceRequests.value?.willNotProvideSurface()
        _surfaceRequests.update { surfaceRequest }
      }
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
}