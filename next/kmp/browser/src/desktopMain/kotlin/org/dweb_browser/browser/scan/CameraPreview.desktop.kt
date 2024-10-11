package org.dweb_browser.browser.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformDirectory
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource

/**
 * 桌面端是选择图片文件
 */
@Composable
actual fun CameraPreviewRender(
  modifier: Modifier,
  controller: SmartScanController,
  resultContent: @Composable () -> Unit
) {
  // 创建渲染动画
//  var startLoading by remember { mutableStateOf(true) }
//
//  LoadingView(startLoading) {}

  // 创建相机控制器
  val cameraController = remember {
    CameraControllerImpl(controller).also { c ->
      controller.cameraController?.stop()
      controller.cameraController = c // 赋值
    }
  }
  // 没有检测到摄像头
//  if (cameraController.webcam == null) {
//    return noWebcamDetected(controller)
//  }

  // TODO 后续需要判断是否有摄像头，来进行处理。
  // AlbumPreviewRender(modifier, controller)
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(BrowserI18nResource.QRCode.tip_no_camera())
    resultContent()
  }

//  LaunchedEffect(Unit) {
//    startLoading = false
//  }
}


/**没有检测到网络摄像头*/
@Composable
private fun noWebcamDetected(controller: SmartScanController) {
  AlertDialog(
    icon = {
      Icon(Icons.Default.Camera, contentDescription = "Example Icon")
    },
    title = {
      Text(text = BrowserI18nResource.QRCode.webcam_detected_title.text)
    },
    text = {
      Text(text = BrowserI18nResource.QRCode.webcam_detected_body.text)
    },
    onDismissRequest = {
    },
    dismissButton = {
      TextButton(
        onClick = {
          controller.onCancel("dismiss")
        }
      ) {
        Text(BrowserI18nResource.QRCode.dismiss.text)
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          controller.updatePreviewType(SmartModuleTypes.Endoscopic)
        }
      ) {
        Text(BrowserI18nResource.QRCode.confirm.text)
      }
    }
  )
}

/**
 * 打开文件选择器
 */
@Composable
actual fun AlbumPreviewRender(
  modifier: Modifier,
  controller: SmartScanController
) {
  // 渲染文件选择
  val scope = rememberCoroutineScope()
  val directory: PlatformDirectory? by remember { mutableStateOf(null) }
  val singleFilePicker = rememberFilePickerLauncher(
    type = PickerType.Image,
    title = BrowserI18nResource.QRCode.select_QR_code.text,
    initialDirectory = directory?.path,
    onResult = { file ->
      file?.let {
        scope.launch {
          val byteArray = file.readBytes()
//          val image = Image.makeFromEncoded(byteArray)
//          // 回调图片
//          controller.albumImageFlow.tryEmit(image.toComposeImageBitmap())
          controller.albumImageFlow.tryEmit(byteArray)
          // 发送给识别模块 // TODO 该操作不应该在文件选择器这边执行，而是应该放到图片渲染后，在进行识别图片
//          globalDefaultScope.launch {
//            controller.decodeQrCode {
//              recognize(byteArray, 0)
//            }
//          }
        }
      } ?: controller.updatePreviewType(SmartModuleTypes.Scanning) // 如果直接返回，没有文件信息，返回空
    }
  )

  LaunchedEffect(Unit) {
    singleFilePicker.launch()
  }
}