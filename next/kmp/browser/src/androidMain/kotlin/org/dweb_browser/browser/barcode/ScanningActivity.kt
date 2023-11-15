package org.dweb_browser.browser.barcode

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import org.dweb_browser.browser.R
import org.dweb_browser.browser.barcode.ui.QRCodeScanView
import org.dweb_browser.browser.barcode.ui.rememberQRCodeScanState
import org.dweb_browser.helper.platform.PureViewController

@OptIn(ExperimentalPermissionsApi::class)
class ScanningActivity : PureViewController() {
  companion object {
    const val IntentFromIPC = "fromIPC"
  }

  private fun String.regexDeepLink() = Regex("dweb:.+").matchEntire(this)?.groupValues?.get(0)

  init {
    onCreate { params ->
      // 隐藏系统工具栏
      val windowInsetsController =
        WindowCompat.getInsetsController(window, window.decorView)
      windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

      val fromIpc = params.getBoolean(IntentFromIPC) ?: false

      addContent {
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
                startActivity(Intent().also {
                  it.`package` = this@ScanningActivity.packageName
                  it.action = Intent.ACTION_VIEW
                  it.data = Uri.parse(dwebLink)
                  it.addCategory("android.intent.category.BROWSABLE")
                  it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
    onStop {
      finish()
    }
  }

}