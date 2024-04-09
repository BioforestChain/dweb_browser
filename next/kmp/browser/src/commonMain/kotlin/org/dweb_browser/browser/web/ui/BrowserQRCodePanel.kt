package org.dweb_browser.browser.web.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.common.barcode.QRCodeScanModel
import org.dweb_browser.browser.common.barcode.QRCodeScanRender
import org.dweb_browser.browser.common.barcode.QRCodeState
import org.dweb_browser.browser.common.barcode.openDeepLink
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.helper.falseAlso
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.window.render.LocalWindowController

@Composable
fun BrowserQRCodePanel(modifier: Modifier) {
  val viewModel = LocalBrowserViewModel.current

  AnimatedVisibility(
    visible = viewModel.showQRCodePanel, modifier = modifier, enter = fadeIn(), exit = fadeOut()
  ) {
    val qrCodeScanModel = remember { QRCodeScanModel() }
    LaunchedEffect(qrCodeScanModel) { // 判断权限，如果已授权直接显示，未授权就进行显示并隐藏
      if (viewModel.requestSystemPermission(
          title = BrowserI18nResource.QRCode.permission_tip_camera_title.text,
          description = BrowserI18nResource.QRCode.permission_tip_camera_message.text,
          permissionName = SystemPermissionName.CAMERA
        )
      ) { // 请求权限
        qrCodeScanModel.updateQRCodeStateUI(QRCodeState.Scanning)
      } else {
        viewModel.showToastMessage(BrowserI18nResource.QRCode.permission_denied.text)
        viewModel.showQRCodePanel = false
      }
    }
    LocalWindowController.current.GoBackHandler {
      when (qrCodeScanModel.state) {
        QRCodeState.CameraCheck, QRCodeState.AlarmCheck -> {
          qrCodeScanModel.updateQRCodeStateUI(QRCodeState.Scanning)
        }

        else -> {
          viewModel.showQRCodePanel = false
        }
      }
    }
    QRCodeScanRender(
      scanModel = qrCodeScanModel,
      requestPermission = {
        viewModel.requestSystemPermission(
          title = BrowserI18nResource.QRCode.permission_tip_camera_title.text,
          description = BrowserI18nResource.QRCode.permission_tip_camera_message.text,
          permissionName = SystemPermissionName.CAMERA
        ).falseAlso {
          viewModel.showToastMessage(BrowserI18nResource.QRCode.permission_denied.text)
          viewModel.showQRCodePanel = false
        }
      },
      onSuccess = { openDeepLink(it); viewModel.showQRCodePanel = false },
      onCancel = { viewModel.showQRCodePanel = false })
  }
}