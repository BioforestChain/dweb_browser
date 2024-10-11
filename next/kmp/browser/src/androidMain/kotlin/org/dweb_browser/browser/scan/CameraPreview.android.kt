package org.dweb_browser.browser.scan

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.AspectRatio
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.surface.ImplementationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.ByteArrayOutputStream

@SuppressLint("RestrictedApi", "UnusedBoxWithConstraintsScope")
@Composable
actual fun CameraPreviewRender(
  modifier: Modifier, controller: SmartScanController, resultContent: @Composable () -> Unit
) {
  Box(modifier.fillMaxSize()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val aspectRatioType = AspectRatio.RATIO_16_9

    // 相册图片选择器
    val imagePickerLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        // 用户选中了图片
        if (uri != null) {
          val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
          val os = ByteArrayOutputStream()
          bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
          controller.albumImageFlow.tryEmit(os.toByteArray())
        }
      }

    // 创建相机控制器
    val cameraController = remember {
      CameraControllerImpl(controller, imagePickerLauncher).also { c ->
        controller.cameraController?.stop()
        controller.cameraController = c // 赋值
      }
    }

    DisposableEffect(Unit) {
      onDispose { cameraController.stop() }
    }

    val currentSurfaceRequest: SurfaceRequest? by cameraController.surfaceRequests.collectAsStateWithLifecycle()
    // CoordinateTransformer for transforming from Offsets to Surface coordinates
    val coordinateTransformer = remember { MutableCoordinateTransformer() }
    LaunchedEffect(coordinateTransformer) {
      cameraController.initializeCamera(context, aspectRatioType, lifecycleOwner)
    }
    currentSurfaceRequest?.let { surfaceRequest ->
      BoxWithConstraints(
        Modifier
          .fillMaxSize()
          .background(Color.Black),
        contentAlignment = Alignment.Center
      ) {
        val density = LocalDensity.current.density
        val maxAspectRatio: Float = maxWidth / maxHeight
        val aspectRatioFloat = when (aspectRatioType) {
          AspectRatio.RATIO_16_9 -> 9.0f / 16f
          AspectRatio.RATIO_4_3 -> 3.0f / 4f
          else -> 9.0f / 16f
        }
        val shouldUseMaxWidth = maxAspectRatio <= aspectRatioFloat
        val width = if (!shouldUseMaxWidth) maxWidth else maxHeight * aspectRatioFloat
        val height = if (shouldUseMaxWidth) maxHeight else maxWidth / aspectRatioFloat
        cameraController.previewSize = Size(width.value * density, height.value * density)

        Box(modifier = Modifier.size(width, height)) {
          CameraXViewfinder(
            surfaceRequest = surfaceRequest,
            implementationMode = ImplementationMode.EXTERNAL,
            modifier = Modifier.fillMaxSize(),
            coordinateTransformer = coordinateTransformer
          )
        }
      }
    }
    resultContent()
  }
}

