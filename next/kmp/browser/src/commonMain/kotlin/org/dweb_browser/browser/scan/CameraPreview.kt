package org.dweb_browser.browser.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import org.dweb_browser.sys.window.core.WindowContentRenderScope

@Composable
fun WindowContentRenderScope.RenderBarcodeScanning(
  modifier: Modifier,
  controller: SmartScanController
) {
  val viewScope = rememberCoroutineScope()
  val isOpenAlbum = mutableStateOf(false)

  // 视图切换,如果扫描到了二维码
  Box(modifier) {
    CameraPreviewRender(
      modifier = modifier,
      controller = controller
    )
    controller.DefaultScanningView(modifier)
    controller.RenderScanResultView(modifier)
  }
}

@Composable
expect fun CameraPreviewRender(
  modifier: Modifier = Modifier,
  controller: SmartScanController
)
