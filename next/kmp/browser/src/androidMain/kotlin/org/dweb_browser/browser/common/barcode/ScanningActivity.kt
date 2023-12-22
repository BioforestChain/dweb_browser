package org.dweb_browser.browser.common.barcode

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.SimpleI18nResource
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
        val showPermissionTips = remember { mutableStateOf(false) }
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

          PermissionTipsView(showPermissionTips)

          LaunchedEffect(qrCodeScanModel) {
            if (!checkPermission(Manifest.permission.CAMERA)) {
              showPermissionTips.value = true
              val result = requestPermissionLauncher.launch(Manifest.permission.CAMERA)
              if (result) {
                showPermissionTips.value = false
                qrCodeScanModel.stateChange.emit(QRCodeState.Scanning)
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
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.primary)
      .padding(16.dp)
  ) {
    Text(
      text = PermissionI18nResource.permission_tip_camera_title(),
      color = MaterialTheme.colorScheme.background
    )
    Text(
      text = PermissionI18nResource.permission_tip_camera_message(),
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.background
    )
  }
}

private object PermissionI18nResource {
  val permission_tip_camera_title = SimpleI18nResource(
    Language.ZH to "相机权限使用说明",
    Language.EN to "Camera Permission Instructions"
  )
  val permission_tip_camera_message = SimpleI18nResource(
    Language.ZH to "DwebBrowser正在向您获取“相机”权限，同意后，将用于为您提供拍照服务",
    Language.EN to "DwebBrowser is asking you for \"Camera\" permissions, and if you agree, it will be used to take photos for you"
  )
}