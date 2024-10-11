package org.dweb_browser.browser.scan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.helper.compose.UnScaleBox
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectNull
import platform.Foundation.NSCoder
import platform.UIKit.UIView

internal class ResizeUIView : UIView {
  @OptIn(ExperimentalForeignApi::class)
  var onResize: ((CValue<CGRect>) -> Unit)? = null

  @OptIn(ExperimentalForeignApi::class)
  override fun layoutSubviews() {
    onResize?.also { it(frame) }
    super.layoutSubviews()
  }

  @Suppress("UNUSED") // required by Objective-C runtime
  @OverrideInit
  constructor(coder: NSCoder) : super(coder) {
    throw UnsupportedOperationException("init(coder: NSCoder) is not supported for ResizeUIView")
  }

  @OptIn(ExperimentalForeignApi::class)
  constructor() : super(frame = CGRectNull.readValue())
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraPreviewRender(
  modifier: Modifier,
  controller: SmartScanController,
  resultContent: @Composable () -> Unit
) {
  val uiViewController = LocalUIViewController.current
  val density = LocalDensity.current.density


  // 创建一个 UIView 作为相机预览的容器
  val uiView = remember { ResizeUIView() }
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
    return controller.isSimulator()
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
    UIKitView(
      factory = {
        uiView.also {
          it.onResize = { frame ->
            cameraController.onResize(frame)
          }
        }
      },
      modifier = Modifier.fillMaxSize(),
    )
    resultContent()
  }
}

@Composable
private fun SmartScanController.isSimulator() {
  AlertDialog(
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
      this.onCancel("确定")
    },
    confirmButton = {
      TextButton(
        onClick = {
          this.onCancel("确定")
        }
      ) {
        Text(BrowserI18nResource.QRCode.confirm.text)
      }
    }
  )
}




