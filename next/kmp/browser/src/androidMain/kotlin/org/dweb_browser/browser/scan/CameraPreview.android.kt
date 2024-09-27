package org.dweb_browser.browser.scan

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.surface.ImplementationMode
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import org.dweb_browser.helper.globalDefaultScope
import java.io.ByteArrayOutputStream
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@SuppressLint("RestrictedApi")
@Composable
actual fun CameraPreviewRender(modifier: Modifier, controller: SmartScanController) {
  BoxWithConstraints(modifier.fillMaxSize()) {
    println("QAQ maxWidth=$maxWidth maxHeight=$maxHeight")
    var previewSize by remember { mutableStateOf(Size(100, 100)) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current.density

    // 相册图片选择器
    val imagePickerLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        // 用户选中了图片
        if (uri != null) {
          val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
          // 调整为合适的大小，不然识别不出来
          val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
          val newWidth: Int
          val newHeight: Int
          if (previewSize.width.toFloat() / previewSize.height.toFloat() > aspectRatio) {
            newHeight = previewSize.height
            newWidth = (newHeight * aspectRatio).toInt()
          } else {
            newWidth = previewSize.width
            newHeight = (newWidth / aspectRatio).toInt()
          }
          val previewBitmap = Bitmap.createScaledBitmap(
            bitmap,
            newWidth,
            newHeight,
            true
          )
          val os = ByteArrayOutputStream()
          previewBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
          controller.albumImageFlow.tryEmit(os.toByteArray())
          // TODO 这边和桌面端一样，不应该在这里进行识别，而是收到上面tryEmit后，界面渲染时，再执行识别
//        globalDefaultScope.launch {
//          controller.decodeQrCode {
//            recognize(previewBitmap, 0)
//          }
//        }
        }
      }

    // 创建相机控制器
    val cameraController = remember {
      CameraControllerImpl(controller, imagePickerLauncher).also { c ->
        controller.cameraController?.stop()
        controller.cameraController = c // 赋值
      }
    }

    LifecycleStartEffect(Unit) {
      cameraController.initializeCamera(context, lifecycleOwner, previewSize)
      onStopOrDispose {
        cameraController.stop()
      }
    }

    val currentSurfaceRequest: SurfaceRequest? by cameraController.surfaceRequests.collectAsStateWithLifecycle()
    println("QAQ currentSurfaceRequest=$currentSurfaceRequest")

    currentSurfaceRequest?.let { surfaceRequest ->
      // CoordinateTransformer for transforming from Offsets to Surface coordinates
      val coordinateTransformer = remember { MutableCoordinateTransformer() }
      previewSize = surfaceRequest.resolution
      val radio = surfaceRequest.resolution.width.toFloat() / surfaceRequest.resolution.height
//    if (previewSize.)

      println("QAQ resolution=${surfaceRequest.resolution} width=${surfaceRequest.resolution.width / density} height=${surfaceRequest.resolution.height / density}")

      println("QAQ previewSize=$previewSize")
//    surfaceRequest.setTransformationInfoListener(Runnable::run) {
//
//    }

      CameraXViewfinder(
        surfaceRequest = surfaceRequest,
        implementationMode = ImplementationMode.EXTERNAL,
        modifier = Modifier.fillMaxSize(),
        coordinateTransformer = coordinateTransformer
      )
    }
  }
}

