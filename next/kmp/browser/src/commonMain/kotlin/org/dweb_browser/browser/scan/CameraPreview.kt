package org.dweb_browser.browser.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.PureViewControllerPlatform
import org.dweb_browser.helper.platform.platform
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController

@Composable
fun WindowContentRenderScope.RenderBarcodeScanning(
  modifier: Modifier, controller: SmartScanController
) {
  // 如果是选中文件
  val selectImg by controller.albumImageFlow.collectAsState()
  // 视图切换,如果扫描到了二维码
  Box(modifier) {
    if (selectImg == null) {
      // 渲染相机内容
      CameraPreviewRender(
        modifier = Modifier.fillMaxSize(), controller = controller
      )
      // 扫描线和打开相册，暂时不再桌面端支持
      when (IPureViewController.platform) {
        PureViewControllerPlatform.Desktop -> {
        }

        PureViewControllerPlatform.Apple, PureViewControllerPlatform.Android -> {
          controller.DefaultScanningView(Modifier.fillMaxSize().zIndex(2f))
        }
      }
      // 渲染扫码结果
      controller.RenderScanResultView(
        Modifier.matchParentSize().zIndex(3f)
      )
    } else {
      LocalWindowController.current.navigation.GoBackHandler {
        controller.albumImageFlow.value = null
      }
      // 如果是选中图片，渲染选中的图片
      controller.RenderCaptureResult(
        Modifier.fillMaxSize(), selectImg!!
      )
    }
  }
}

/**相机preview视图*/
@Composable
expect fun CameraPreviewRender(
  modifier: Modifier = Modifier, controller: SmartScanController
)
