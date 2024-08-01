package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.desk.AlertController
import org.dweb_browser.helper.compose.CommonI18n
import org.dweb_browser.helper.compose.collectAsMutableState

/**
 * 错误信息
 */
@Composable
fun AlertController.Render() {
  var alertMessages by alertMessagesFlow.collectAsMutableState()
  // 每次只显示一个
  alertMessages.firstOrNull()?.also { message ->
    key(message) {
      val dismissHandler: () -> Unit = {
        alertMessages -= message
      }
      AlertDialog(
        onDismissRequest = dismissHandler,
        icon = {
          Icon(Icons.TwoTone.Error, contentDescription = "error")
        },
        title = {
          Text(message.title ?: CommonI18n.error(), color = MaterialTheme.colorScheme.error)
        },
        text = {
          Text(
            message.message,
            modifier = Modifier.verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodySmall,
          )
        },
        containerColor = MaterialTheme.colorScheme.errorContainer,
        confirmButton = {
          Button(onClick = dismissHandler) {
            Text(CommonI18n.close())
          }
        },
      )
    }
  }
}