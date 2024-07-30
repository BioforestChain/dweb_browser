package org.dweb_browser.browser.scan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.platform.LocalDensity
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.dwebview.UnScaleBox
import org.dweb_browser.sys.window.render.LocalWindowContentStyle
import org.dweb_browser.sys.window.render.UIKitViewInWindow
import platform.UIKit.UIView


@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraPreviewRender(
  modifier: Modifier,
  controller: SmartScanController
) {
  val uiViewController = LocalUIViewController.current
  val density = LocalDensity.current.density
  // 创建一个 UIView 作为相机预览的容器
  val uiView = remember { UIView() }
  // 创建相机控制器
  val cameraController = remember(uiViewController) {
    try {
      val c = CameraControllerImpl(controller, uiViewController, uiView, density)
      controller.cameraController?.stop()
      controller.cameraController = c // 赋值
      return@remember c
    } catch (e: Exception) {
      return@remember null
    }
  }
  if (cameraController == null) {
    return isSimulator(modifier, controller)
  }

  DisposableEffect(Unit) {
    // 开启捕获
    cameraController.triggerCapture()
    onDispose {
      cameraController.stop()
    }
  }
  // 对冲窗口样式
  val scale by controller.scaleFlow.collectAsState()
  UnScaleBox(scale, modifier) {
    uiView.UIKitViewInWindow(
      modifier = Modifier.fillMaxSize(),
      style = LocalWindowContentStyle.current.frameStyle,
      onResize = { _, frame ->
        cameraController.onResize(frame)
      }
    )
  }
}

@Composable
private fun isSimulator(modifier: Modifier, controller: SmartScanController) {
  AlertDialog(
    modifier = modifier,
    icon = {
      Icon(Icons.Default.Info, contentDescription = "Example Icon")
    },
    title = {
      Text(text = BrowserI18nResource.QRCode.simulator_title.text)
    },
    text = {
      Text(
        text = BrowserI18nResource.QRCode.simulator_body.text.trimIndent()
      )
    },
    onDismissRequest = {
      controller.onCancel("确定")
    },
    confirmButton = {
      TextButton(
        onClick = {
          controller.onCancel("确定")
        }
      ) {
        Text(BrowserI18nResource.QRCode.confirm.text)
      }
    }
  )
}




