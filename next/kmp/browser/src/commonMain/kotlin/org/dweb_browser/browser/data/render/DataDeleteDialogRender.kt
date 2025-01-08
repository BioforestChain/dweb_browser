package org.dweb_browser.browser.data.render

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.data.DataController
import org.dweb_browser.browser.data.DataI18n
import org.dweb_browser.helper.compose.CommonI18n
import org.dweb_browser.helper.compose.collectAsMutableState

@Composable
fun DataController.DeleteDialogRender() {
  val deleteProfile by deleteProfileFlow.collectAsState()
  when (val profileInfo = deleteProfile) {
    null -> {}
    else -> {
      var deleteJob by deleteJobFlow.collectAsMutableState()
      val isRunning by isRunningFlow.collectAsState()
      val appName = when (profileInfo) {
        is DataController.ProfileBase -> profileInfo.mmid
        is DataController.ProfileDetail -> profileInfo.short_name
      }
      AlertDialog(
        { closeDeleteDialog() },
        title = {
          when {
            isRunning -> Text(DataI18n.uninstall_running_app_title())
            else -> Text(CommonI18n.warning())
          }
        },
        text = {
          when {
            isRunning -> Text(DataI18n.uninstall_running_app_tip(appName))
            else -> Text(DataI18n.uninstall_app_tip (appName))
          }
        },
        confirmButton = {
          FilledTonalButton(
            {
              deleteJobFlow.value = storeNMM.scopeLaunch(cancelable = true) {
                storeNMM.bootstrapContext.dns.close(profileInfo.mmid)
                deleteProfile(profileInfo)
                closeDeleteDialog()
              }
            },
            enabled = deleteJob == null,
            colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.error),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              when (deleteJob) {
                null -> Icon(
                  Icons.TwoTone.DeleteForever, contentDescription = "kill and delete profile"
                )

                else -> CircularProgressIndicator(
                  modifier = Modifier.size(24.dp),
                  color = MaterialTheme.colorScheme.secondary,
                  trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
              }
              Text("关停并清除")
            }

          }
        },
        dismissButton = {
          Button({
            deleteJob?.cancel()
            deleteJob = null
            closeDeleteDialog()
          }) {
            Text(CommonI18n.cancel())
          }
        },
      )
    }
  }
}
