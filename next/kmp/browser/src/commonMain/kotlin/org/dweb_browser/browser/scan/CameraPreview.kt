package org.dweb_browser.browser.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
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
      modifier = Modifier.fillMaxSize(),
      controller = controller
    )
    controller.DefaultScanningView(Modifier.fillMaxSize().zIndex(2f))
    controller.RenderScanResultView(Modifier.matchParentSize().zIndex(3f))
  }
}

@Composable
expect fun CameraPreviewRender(
  modifier: Modifier = Modifier,
  controller: SmartScanController
)
