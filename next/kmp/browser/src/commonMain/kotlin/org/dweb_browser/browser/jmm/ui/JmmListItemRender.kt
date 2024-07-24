package org.dweb_browser.browser.jmm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.twotone.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.jmm.JmmI18n
import org.dweb_browser.browser.jmm.JmmI18nResource
import org.dweb_browser.browser.jmm.JmmMetadata
import org.dweb_browser.browser.jmm.JmmStatus
import org.dweb_browser.helper.compose.CommonI18n
import org.dweb_browser.helper.compose.SwipeToViewBox
import org.dweb_browser.helper.compose.hoverCursor
import org.dweb_browser.helper.compose.rememberSwipeToViewBoxState
import org.dweb_browser.helper.formatDatestampByMilliseconds
import org.dweb_browser.helper.toSpaceSize

@Composable
fun JmmListItem(
  jmmMetadata: JmmMetadata,
  onRemove: () -> Unit,
  onUnInstall: () -> Unit,
  onOpenDetail: () -> Unit,
) {
  val state = rememberSwipeToViewBoxState()
  SwipeToViewBox(
    state,
    backgroundContent = {
      Row {
        var showUnInstallAlert by remember { mutableStateOf(false) }
        if (showUnInstallAlert) {
          val scope = rememberCoroutineScope()
          AlertDialog(
            onDismissRequest = { showUnInstallAlert = false },
            icon = {
              Icon(
                Icons.TwoTone.DeleteForever,
                null,
                tint = MaterialTheme.colorScheme.error
              )
            },
            title = { Text(JmmI18n.uninstall_alert_title()) },
            text = {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(jmmMetadata.manifest.short_name)
                Text(jmmMetadata.manifest.id)
              }
            },
            confirmButton = {
              FilledTonalButton(
                onUnInstall,
                colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = MaterialTheme.colorScheme.errorContainer,
                  contentColor = MaterialTheme.colorScheme.error,
                )
              ) {
                Text(JmmI18n.confirm_uninstall())
              }
            },
            dismissButton = {
              TextButton({
                showUnInstallAlert = false
                scope.launch { state.close() }
              }) {
                Text(CommonI18n.cancel())
              }
            },
          )
        }
        val removeColors = ButtonDefaults.textButtonColors(
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.error,
        )
        when (jmmMetadata.state.state) {
          JmmStatus.INSTALLED -> TextButton(
            onClick = { showUnInstallAlert = true },
            modifier = Modifier.fillMaxHeight(),
            colors = removeColors,
            shape = RectangleShape,
          ) {
            Text(text = JmmI18nResource.uninstall(), modifier = Modifier.padding(8.dp))
          }

          else -> TextButton(
            onClick = onRemove,
            modifier = Modifier.fillMaxHeight(),
            colors = removeColors,
            shape = RectangleShape
          ) {
            Text(text = JmmI18nResource.remove_record(), modifier = Modifier.padding(8.dp))
          }
        }
      }
    },
  ) {
    ListItem(
      headlineContent = {
        Text(
          text = jmmMetadata.manifest.short_name,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.titleMedium,
        )
      },
      modifier = Modifier.hoverCursor().clickable {
        onOpenDetail()
      },
      supportingContent = {
        Column {
          Row {
            Text("v", fontWeight = FontWeight.Bold)
            Text(jmmMetadata.manifest.version)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              jmmMetadata.manifest.bundle_size.toSpaceSize(),
              style = MaterialTheme.typography.bodySmall
            )
            Text(
              jmmMetadata.installTime.formatDatestampByMilliseconds(),
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      },
      leadingContent = {
        Box(modifier = Modifier.height(72.dp), contentAlignment = Alignment.Center) {
          jmmMetadata.manifest.IconRender()
        }
      },
      trailingContent = {
        val scope = rememberCoroutineScope()
        IconButton({
          scope.launch {
            state.open()
          }
        }) {
          Icon(Icons.Default.MoreHoriz, "more")
        }
      },
    )
  }
}