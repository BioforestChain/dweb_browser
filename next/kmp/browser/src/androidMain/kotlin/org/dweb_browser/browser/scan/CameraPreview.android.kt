package org.dweb_browser.browser.scan

import android.graphics.BitmapFactory
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner


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
  AndroidView(factory = {
    previewView.layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT,
    )
    previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

    previewView
  }, modifier = modifier)
}
