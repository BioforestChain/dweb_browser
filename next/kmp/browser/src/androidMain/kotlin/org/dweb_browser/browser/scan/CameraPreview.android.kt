package org.dweb_browser.browser.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream


/**这里是android相机的预览画到Compose上，并且输出扫描到的一贞一贞图片*/
@OptIn(ExperimentalGetImage::class)
@Composable
actual fun CameraPreviewRender(
  modifier: Modifier, controller: SmartScanController
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val previewView = remember { PreviewView(context) }

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
        if (previewView.width.toFloat() / previewView.height.toFloat() > aspectRatio) {
          newHeight = previewView.height
          newWidth = (newHeight * aspectRatio).toInt()
        } else {
          newWidth = previewView.width
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
    // 设置显示模式为兼容，防止页面抖动
    previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    previewView
  }, modifier = modifier.fillMaxSize())
}
