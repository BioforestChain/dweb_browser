package org.dweb_browser.browser.common.barcode

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.sys.permission.PermissionTipsView
import org.dweb_browser.sys.window.render.NativeBackHandler

class ScanningActivity : ComponentActivity() {
  private var showTips by mutableStateOf(false)
  private val qrCodeScanModel = QRCodeScanModel()
  private val permissionResult = PromiseOut<Boolean>()
  private val launcherRequestPermission =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
      permissionResult.resolve(result)
      if (result) showTips = false else finish()
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)

    setContent {
      DwebBrowserAppTheme {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
          NativeBackHandler {
            when (qrCodeScanModel.state) {
              QRCodeState.MultiSelect -> qrCodeScanModel.updateQRCodeStateUI(QRCodeState.Scanning)
              else -> {
                qrCodeScanModel.updateQRCodeStateUI(QRCodeState.Hide)
                finish()
              }
            }
          }
          QRCodeScanRender(
            scanModel = qrCodeScanModel,
            requestPermission = {
              if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                showTips = true
                launcherRequestPermission.launch(Manifest.permission.CAMERA)
                permissionResult.waitPromise()
              } else true
            },
            onSuccess = { data ->
              if (!openDeepLink(data, showBackground = true)) {
                qrCodeScanModel.updateQRCodeStateUI(QRCodeState.Scanning)
              } else finish()
            },
            onCancel = { finish() }
          )

          if (showTips) {
            PermissionTipsView(
              title = BrowserI18nResource.QRCode.permission_tip_camera_title(),
              description = BrowserI18nResource.QRCode.permission_tip_camera_message()
            )
          }
        }
      }
    }
  }

  override fun onStop() {
    super.onStop()
    finish()
  }
}
