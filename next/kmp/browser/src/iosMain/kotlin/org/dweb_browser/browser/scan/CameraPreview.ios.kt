package org.dweb_browser.browser.scan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.dwebview.UnScaleBox
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.sys.window.render.LocalWindowContentStyle
import org.dweb_browser.sys.window.render.UIKitViewInWindow
import platform.UIKit.UIView


@Composable
actual fun CameraPreviewRender(
  modifier: Modifier,
  controller: SmartScanController
) {

  // 判断是否在模拟器执行
  if (PureViewController.isSimulator) {
    Text(
      """
      Camera is not available on simulator.
      Please try to run on a real iOS device.
      """.trimIndent(),
      color = Color.White,
      modifier = modifier,
      textAlign = TextAlign.Center
    )
  } else {
    RealDeviceCamera(modifier, controller)
  }
}


@OptIn(ExperimentalForeignApi::class)
@Composable
private fun RealDeviceCamera(
  modifier: Modifier,
  controller: SmartScanController
) {
  val uiViewController = LocalUIViewController.current
  val density = LocalDensity.current.density
  // 创建一个 UIView 作为相机预览的容器
  val uiView = remember { UIView() }
  // 创建相机控制器
  val cameraController = remember(uiViewController) {
    val c = CameraControllerImpl(controller, uiViewController, uiView, density)
    controller.cameraController?.stop()
    controller.cameraController = c // 赋值
    c
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


