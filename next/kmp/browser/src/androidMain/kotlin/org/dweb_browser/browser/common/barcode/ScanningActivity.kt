package org.dweb_browser.browser.common.barcode

import android.Manifest
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.platform.PureViewController

class ScanningActivity : PureViewController() {
  companion object {
    const val IntentFromIPC = "fromIPC"
  }

  init {
    onCreate { params ->
      // 隐藏系统工具栏
      val windowInsetsController =
        WindowCompat.getInsetsController(window, window.decorView)
      windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

      val fromIpc = params.getBoolean(IntentFromIPC) ?: false

      addContent {
        val qrCodeScanModel = remember { QRCodeScanModel() }
        LocalCompositionChain.current.Provider(
          LocalQRCodeModel provides qrCodeScanModel
        ) {
          QRCodeScanView(
            onSuccess = { data ->
              if (!fromIpc) {
                openDeepLink(data)
              }
              finish()
            },
            onCancel = { finish() }
          )
        }

        LaunchedEffect(qrCodeScanModel) {
          val result = requestPermissionLauncher.launch(Manifest.permission.CAMERA)
          if (result) {
            qrCodeScanModel.stateChange.emit(QRCodeState.Scanning)
          }
        }
      }
    }
    onStop {
      finish()
    }
  }

}