package org.dweb_browser.sys.keychain.render

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.CommonI18n
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.sys.keychain.KeychainI18nResource
import org.dweb_browser.sys.keychain.KeychainManager

@Composable
fun KeychainManager.DetailManager.KeyManager.PasswordDeleteView(
  modifier: Modifier = Modifier,
  onDismissRequest: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  var deleting by remember { mutableStateOf(false) }
  AlertDialog(
    onDismissRequest,
    modifier = modifier,
    title = { Text(CommonI18n.warning()) },
    text = { Text(KeychainI18nResource.password_delete_tip(key)) },
    dismissButton = {
      TextButton(onDismissRequest, enabled = !deleting) {
        Text(CommonI18n.cancel())
      }
    },
    confirmButton = {
      Button(
        {
          scope.launch {
            deleting = true
            runCatching {
              deletePassword(key)
            }.getOrDefault(false).trueAlso {
              onDismissRequest()
            }
            deleting = false
          }
        },
        enabled = !deleting,
        colors = ButtonDefaults.buttonColors().copy(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError,
        ),
      ) {
        Text(CommonI18n.delete())
      }
    },
  )
}