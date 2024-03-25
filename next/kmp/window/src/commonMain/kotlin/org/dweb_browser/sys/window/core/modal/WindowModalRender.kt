package org.dweb_browser.sys.window.core.modal

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.dweb_browser.sys.window.WindowI18nResource


@Composable
internal expect fun ModalState.RenderCloseTipImpl(onConfirmToClose: () -> Unit)

@Composable
internal fun ModalState.CommonRenderCloseTip(onConfirmToClose: () -> Unit) {
  AlertDialog(
    onDismissRequest = {
      showCloseTip.value = ""
    },
    title = {
      Text(
        text = when (this) {
          is AlertModalState -> WindowI18nResource.modal_close_alert_tip()
          is BottomSheetsModalState -> WindowI18nResource.modal_close_bottom_sheet_tip()
        }
      )
    },
    text = { Text(text = showCloseTip.value) },
    confirmButton = {
      ElevatedButton(onClick = { showCloseTip.value = "" }) {
        Text(WindowI18nResource.modal_close_tip_keep())
      }
    },
    dismissButton = {
      Button(onClick = {
        onConfirmToClose()
        // showCloseTip.value = "";
      }) {
        Text(WindowI18nResource.modal_close_tip_close())
      }
    },
  )
}
