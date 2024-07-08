package org.dweb_browser.browser.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.withRenderScope
import org.dweb_browser.sys.window.render.NativeBackHandler

@Composable
fun ScanStdController.Render(
  modifier: Modifier, windowContentRenderScope: WindowContentRenderScope
) {
  val qrCodeScanModel = remember { QRCodeScanModel() }
  NativeBackHandler {
    when (qrCodeScanModel.state) {
      QRCodeState.CameraCheck, QRCodeState.AlarmCheck, QRCodeState.AnalyzePhoto -> {
        qrCodeScanModel.updateQRCodeStateUI(QRCodeState.Scanning)
      }

      else -> closeWindow()
    }
  }
  Box(modifier = modifier.withRenderScope(windowContentRenderScope)) {
    QRCodeScanRender(
      scanModel = qrCodeScanModel,
      requestPermission = { true },
      onSuccess = { onSuccess(it) },
      onCancel = { onCancel(it) }
    )
  }
}