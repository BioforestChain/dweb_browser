package info.bagen.dwebbrowser.microService.sys.barcodeScanning

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import info.bagen.dwebbrowser.microService.sys.barcodeScanning.ui.QRCodeScanView
import info.bagen.dwebbrowser.microService.sys.barcodeScanning.ui.rememberQRCodeScanState
import org.dweb_browser.core.module.BaseThemeActivity
import org.dweb_browser.helper.compose.theme.DwebBrowserAppTheme

class ScanningActivity : BaseThemeActivity() {

  @OptIn(ExperimentalPermissionsApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // 隐藏系统工具栏
    val windowInsetsController =
      WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    //WindowCompat.setDecorFitsSystemWindows(window, false)

    setContent {
      DwebBrowserAppTheme {
        val qrCodeScanState = rememberQRCodeScanState()
        LaunchedEffect(Unit) { qrCodeScanState.show() }

        QRCodeScanView(
          qrCodeScanState = qrCodeScanState,
          onClose = {
            ScanningController.controller.scanData = ""
            finish()
          },
          onDataCallback = { data ->
            ScanningController.controller.scanData = data
            finish()
          })
      }
    }
  }
}