package info.bagen.dwebbrowser.microService.browser

import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import info.bagen.dwebbrowser.microService.browser.BrowserNMM.Companion.browserController
import info.bagen.dwebbrowser.microService.helper.ioAsyncExceptionHandler
import info.bagen.dwebbrowser.ui.browser.ios.BrowserIntent
import info.bagen.dwebbrowser.ui.browser.ios.BrowserView
import info.bagen.dwebbrowser.ui.camera.QRCodeIntent
import info.bagen.dwebbrowser.ui.camera.QRCodeScanning
import info.bagen.dwebbrowser.ui.camera.QRCodeScanningView
import info.bagen.dwebbrowser.ui.camera.QRCodeViewModel
import info.bagen.dwebbrowser.ui.loading.LoadingView
import info.bagen.dwebbrowser.ui.theme.RustApplicationTheme
import info.bagen.dwebbrowser.util.permission.PermissionManager
import info.bagen.dwebbrowser.util.permission.PermissionUtil

class BrowserActivity : AppCompatActivity() {
  fun getContext() = this
  val qrCodeViewModel: QRCodeViewModel = QRCodeViewModel()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    browserController.activity = this
    setContent {
      WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
        !isSystemInDarkTheme() // 设置状态栏颜色跟着主题走
      RustApplicationTheme {
        browserController.effect(activity = this@BrowserActivity)
        Box(modifier = Modifier.fillMaxSize()) {
          BrowserView(viewModel = browserController.browserViewModel)
          QRCodeScanningView(this@BrowserActivity, qrCodeViewModel)
          LoadingView(browserController.showLoading)
        }
      }
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<out String>, grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == PermissionManager.MY_PERMISSIONS) {
      PermissionManager(this@BrowserActivity)
        .onRequestPermissionsResult(requestCode,
          permissions,
          grantResults,
          object :
            PermissionManager.PermissionCallback {
            override fun onPermissionGranted(
              permissions: Array<out String>, grantResults: IntArray
            ) {
              // openScannerActivity()
              qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(true))
            }

            override fun onPermissionDismissed(permission: String) {
            }

            override fun onNegativeButtonClicked(dialog: DialogInterface, which: Int) {
            }

            override fun onPositiveButtonClicked(dialog: DialogInterface, which: Int) {
              PermissionUtil.openAppSettings()
            }
          })
    } else if (requestCode == QRCodeScanning.CAMERA_PERMISSION_REQUEST_CODE) {
      grantResults.forEach {
        if (it != PackageManager.PERMISSION_GRANTED) return
      }
      qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(true))
    }
  }

  override fun onStop() {
    super.onStop()
    if (browserController.showLoading.value) { // 如果已经跳转了，这边直接改为隐藏
      browserController.showLoading.value = false
    }
  }

  override fun onDestroy() {
    // 退出APP关闭服务
    super.onDestroy()
    browserController.activity = null
  }
}