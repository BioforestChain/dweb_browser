package org.dweb_browser.helper.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun NativeDialog(
  onCloseRequest: () -> Unit,
  properties: NativeDialogProperties,
  setContent: @Composable () -> Unit,
) {
  BasicAlertDialog(
    onCloseRequest,
    properties = DialogProperties(
      dismissOnClickOutside = !properties.modal,
      dismissOnBackPress = !properties.modal,
    ),
    content = {
      Column(Modifier.sizeIn(minWidth = 120.dp)) {
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(8.dp)
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            properties.icon?.also { icon ->
              Image(icon, properties.title, modifier = Modifier.size(32.dp))
            }
            properties.title?.also { title ->
              Text(
                title,
                style = MaterialTheme.typography.titleMedium
              )
            }
          }

          IconButton(onCloseRequest) {
            Icon(Icons.Default.Close, "close window")
          }
        }
        setContent()
      }
    }
  )
}