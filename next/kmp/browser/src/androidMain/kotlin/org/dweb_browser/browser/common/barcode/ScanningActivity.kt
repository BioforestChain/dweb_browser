package org.dweb_browser.browser.common.barcode

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.platform.PureViewController

class ScanningActivity : PureViewController() {
  companion object {
    const val IntentFromIPC = "fromIPC"
  }

  private val showTips = mutableStateOf(false)

  init {
    onCreate { params ->
      // 隐藏系统工具栏
//      val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
//      windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

      val fromIpc = params.getBoolean(IntentFromIPC) ?: false

      addContent {
        val qrCodeScanModel = remember { QRCodeScanModel() }
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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

          PermissionTipsView(showTips)

          LaunchedEffect(qrCodeScanModel) {
            if (!checkPermission(Manifest.permission.CAMERA)) {
              showTips.value = true
              val result = requestPermissionLauncher.launch(Manifest.permission.CAMERA)
              if (result) {
                showTips.value = false
                qrCodeScanModel.stateChange.emit(QRCodeState.Scanning)
              } else {
                finish()
              }
            } else {
              qrCodeScanModel.stateChange.emit(QRCodeState.Scanning)
            }
          }
        }
      }
    }
    onStop {
      finish()
    }
  }
}

@Composable
private fun PermissionTipsView(show: MutableState<Boolean>) {
  if (!show.value) return
  org.dweb_browser.sys.permission.PermissionTipsView(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    title = QRCodeI18nResource.permission_tip_camera_title(),
    description = QRCodeI18nResource.permission_tip_camera_message()
  )
}