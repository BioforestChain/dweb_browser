package info.bagen.dwebbrowser.microService.sys.barcodeScanning

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.microService.sys.barcodeScanning.ui.QRCodeScanView
import info.bagen.dwebbrowser.microService.sys.barcodeScanning.ui.rememberQRCodeScanState
import info.bagen.dwebbrowser.microService.sys.deepLink.DeepLinkActivity
import info.bagen.dwebbrowser.microService.sys.deepLink.regexDeepLink
import org.dweb_browser.core.module.BaseThemeActivity
import org.dweb_browser.helper.compose.theme.DwebBrowserAppTheme

class ScanningActivity : BaseThemeActivity() {
  companion object {
    const val IntentFromIPC = "fromIPC"
  }

  @OptIn(ExperimentalPermissionsApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // 隐藏系统工具栏
    val windowInsetsController =
      WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

    val fromIpc = intent.getBooleanExtra(IntentFromIPC, false)

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
            if (!fromIpc) {
              data.regexDeepLink()?.let { dwebLink ->
                startActivity(Intent(this@ScanningActivity, DeepLinkActivity::class.java).also {
                  it.action = Intent.ACTION_VIEW
                  it.data = Uri.parse(dwebLink)
                  it.addCategory("android.intent.category.BROWSABLE")
                })
              } ?: Toast.makeText(
                this@ScanningActivity,
                getString(R.string.shortcut_toast) + data,
                Toast.LENGTH_SHORT
              ).show()
            }
            finish()
          })
      }
    }
  }

  override fun onStop() {
    super.onStop()
    finish()
  }
}