package org.dweb_browser.browser.scan

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
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
  private var cameraProvider by mutableStateOf<ProcessCameraProvider?>(null)

  // 当前相机实例
  private var camera: Camera? = null

  // 摄像头选择
  private val cameraSelector =
    CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

  private val _surfaceRequests = MutableStateFlow<SurfaceRequest?>(null)
  val surfaceRequests: StateFlow<SurfaceRequest?>
    get() = _surfaceRequests.asStateFlow()

  var previewSize by mutableStateOf(androidx.compose.ui.geometry.Size.Zero)

  // 初始化相机
  suspend fun initializeCamera(
    context: Context, aspectRatio: Int, lifecycleOwner: LifecycleOwner
  ) {
    cameraProvider = ProcessCameraProvider.awaitInstance(context)
    bindPreview(cameraProvider!!, aspectRatio, lifecycleOwner)
  }

  private fun bindPreview(
    cameraProvider: ProcessCameraProvider, aspectRatio: Int, lifecycleOwner: LifecycleOwner
  ) {
    val preview = Preview.Builder().build()
    val resolutionSelector = when (aspectRatio) {
      AspectRatio.RATIO_16_9 -> AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
      AspectRatio.RATIO_4_3 -> AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
      else -> {
        AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
      }
    }

    val imageAnalyzer = ImageAnalysis.Builder()
      .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // 仅将最新图像传送到分析器，并在图像到达时丢弃它们。
      .setResolutionSelector(
        ResolutionSelector.Builder().setAspectRatioStrategy(resolutionSelector).build()
      )
      .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) // 设置输出格式
      .build()

    // 设置异步将获取的图片，进行二维码识别操作
    imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
      globalDefaultScope.launch {
        controller.decodeQrCode {
          // 默认ImageAnalysis获取的图标和界面显示的有90°的差异，所以如果要解析，需要旋转后再进行解析
          val zoomPoint = previewSize.width / imageProxy.height // 结合上面注释，这边也需要修改宽高
          val inputImage = InputImage.fromBitmap(imageProxy.toBitmap(), 90)
          recognize(inputImage, zoomPoint = zoomPoint)
        }
        imageProxy.close()
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
    cameraProvider?.unbindAll()
  }
}