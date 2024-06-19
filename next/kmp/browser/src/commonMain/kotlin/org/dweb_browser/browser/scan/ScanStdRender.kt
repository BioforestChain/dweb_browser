package org.dweb_browser.browser.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.common.barcode.QRCodeScanModel
import org.dweb_browser.browser.common.barcode.QRCodeScanRender
import org.dweb_browser.browser.common.barcode.QRCodeState
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.render.NativeBackHandler

@Composable
fun ScanStdController.ScanStdRender(
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
  Box(modifier = with(windowContentRenderScope) {
    modifier.requiredSize((width / scale).dp, (height / scale).dp).scale(scale)
  }) {
    QRCodeScanRender(
      scanModel = qrCodeScanModel,
      requestPermission = { true },
      onSuccess = { callScanResult(it) },
      onCancel = { closeWindow() }
    )
  }
}