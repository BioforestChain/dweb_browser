package org.dweb_browser.sys.keychain.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.twotone.Key
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.CommonI18n
import org.dweb_browser.helper.compose.SwipeToViewBox
import org.dweb_browser.helper.compose.rememberSwipeToViewBoxState
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.sys.keychain.KeychainI18nResource
import org.dweb_browser.sys.keychain.KeychainManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeychainManager.DetailManager.KeyItemView(
  key: String,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  var passwordRwMode by remember { mutableStateOf(PasswordReadWriteMode.Readonly) }
  var passwordDeleteAlert by remember { mutableStateOf(false) }
  val swapState = rememberSwipeToViewBoxState()
  fun closeSwipe() {
    scope.launch {
      swapState.close()
    }
  }
  SwipeToViewBox(swapState,
    backgroundContent = {
      Row {
        TextButton({
          passwordDeleteAlert = true
        }, Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.error)) {
          Text(
            KeychainI18nResource.password_action_delete(), color = MaterialTheme.colorScheme.onError
          )
        }
        TextButton({
          scope.launch {
            passwordRwMode = PasswordReadWriteMode.ReadWrite
            password = getPassword(key)
          }
        }, Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.primary)) {
          Text(
            KeychainI18nResource.password_action_edit(), color = MaterialTheme.colorScheme.onPrimary
          )
        }
        TextButton({
          scope.launch {
            passwordRwMode = PasswordReadWriteMode.Readonly
            password = getPassword(key)
          }
        }, Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.secondary)) {
          Text(
            KeychainI18nResource.password_action_view(),
            color = MaterialTheme.colorScheme.onSecondary
          )
        }
        TextButton(
          { closeSwipe() },
          Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.tertiary)
        ) {
          Text(CommonI18n.cancel(), color = MaterialTheme.colorScheme.onTertiary)
        }
      }
    }) {
    ListItem(leadingContent = {
      Icon(Icons.TwoTone.Key, null)
    }, headlineContent = { Text(key) }, trailingContent = {
      IconButton({
        scope.launch { swapState.toggle() }
      }) {
        Icon(Icons.Default.MoreHoriz, "more")
      }
    })
  }
  password?.also { passwordSource ->
    BasicAlertDialog({
      password = null
    }, Modifier.wrapContentSize()) {
      ElevatedCard {
        Box(Modifier.zIndex(2f).align(Alignment.End)) {
          IconButton({ password = null }) {
            Icon(Icons.Default.Close, "close dialog")
          }
        }
        PasswordView(key, passwordSource, passwordRwMode)
      }
    }
  }
  passwordDeleteAlert.trueAlso {
    PasswordDeleteView(key) {
      passwordDeleteAlert = false
    }
  }
}