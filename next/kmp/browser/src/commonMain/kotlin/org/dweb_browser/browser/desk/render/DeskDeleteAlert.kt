package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.DeskI18n
import org.dweb_browser.browser.desk.model.DesktopAppModel
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.helper.compose.CommonI18n
import org.dweb_browser.helper.platform.theme.LocalColorful
import org.dweb_browser.sys.window.ext.AlertDeleteDialog
import org.dweb_browser.sys.window.render.AppLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeskDeleteAlert(
  app: DesktopAppModel,
  microModule: NativeMicroModule.NativeRuntime,
  onDismissRequest: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDeleteDialog(
    onDismissRequest = onDismissRequest, onDelete = onConfirm,
    title = {
      when {
        app.isWebLink -> Text(DeskI18n.delete_weblink_title())
        else -> Text(DeskI18n.delete_app_title())
      }
    },
    message = {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          AppLogo.from(app.icon, fetchHook = microModule.blobFetchHook)
            .toDeskAppIcon(containerAlpha = 1f).Render(Modifier.size(36.dp))
          Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(app.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            when (val webLink = app.webLink) {
              null -> Text(
                app.mmid,
                style = MaterialTheme.typography.bodySmall.run { copy(fontSize = fontSize * 0.8f) },
                fontStyle = FontStyle.Italic,
                color = LocalColorful.current.Cyan.current,
              )

              else -> Text(
                webLink,
                style = MaterialTheme.typography.bodySmall.run { copy(fontSize = fontSize * 0.8f) },
                fontStyle = FontStyle.Italic,
                color = LocalColorful.current.Blue.current,
              )
            }
          }
        }

        if (!app.isWebLink) {
          Text(DeskI18n.delete_app_tip(), modifier = Modifier.padding(top = 4.dp))
        }
      }
    },
    deleteText = CommonI18n.confirm(),
    jobCancelable = true,
  )
}