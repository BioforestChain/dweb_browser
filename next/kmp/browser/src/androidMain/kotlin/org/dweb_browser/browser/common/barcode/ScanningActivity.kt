package org.dweb_browser.browser.common.barcode

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme

class ScanningActivity : ComponentActivity() {
  companion object {
    const val IntentFromIPC = "fromIPC"
  }

  private val showTips = mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val fromIpc = intent.getBooleanExtra(IntentFromIPC, false)

    setContent {
      DwebBrowserAppTheme {
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
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
              showTips.value = true
              registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                  showTips.value = false
                  lifecycleScope.launch {
                    qrCodeScanModel.stateChange.emit(QRCodeState.Scanning)
                  }
                } else {
                  finish()
                }
              }.launch(Manifest.permission.CAMERA)
            } else {
              qrCodeScanModel.stateChange.emit(QRCodeState.Scanning)
            }
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

@Composable
private fun PermissionTipsView(show: MutableState<Boolean>) {
  if (!show.value) return
  org.dweb_browser.sys.permission.PermissionTipsView(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    title = QRCodeI18nResource.permission_tip_camera_title(),
    description = QRCodeI18nResource.permission_tip_camera_message()
  )
}