package org.dweb_browser.sys.window.core.modal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController
import org.dweb_browser.sys.window.WindowI18nResource
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert

@Composable
internal actual fun ModalState.RenderCloseTipImpl(onConfirmToClose: () -> Unit) {
  val uiViewController = LocalUIViewController.current
  val alertController = remember {
    UIAlertController.alertControllerWithTitle(
      when (this) {
        is AlertModalState -> WindowI18nResource.modal_close_alert_tip.text
        is BottomSheetsModalState -> WindowI18nResource.modal_close_bottom_sheet_tip.text
      },
      showCloseTip.value,
      UIAlertControllerStyleAlert
    ).also {
      // 保留Modal
      it.addAction(
        UIAlertAction.actionWithTitle(
          WindowI18nResource.modal_close_tip_keep.text,
          UIAlertActionStyleDefault
        ) {
          showCloseTip.value = ""
        })
      // 关闭Modal
      it.addAction(
        UIAlertAction.actionWithTitle(
          WindowI18nResource.modal_close_tip_close.text,
          UIAlertActionStyleCancel
        ) {
          onConfirmToClose()
        })
    }
  }
  DisposableEffect(Unit) {
    uiViewController.presentViewController(alertController, true, null);
    onDispose {
      alertController.dismissViewControllerAnimated(true) {
        showCloseTip.value = ""
      }
    }
  }
}
