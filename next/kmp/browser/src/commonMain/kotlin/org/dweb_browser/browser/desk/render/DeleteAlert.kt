package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.desk.model.DesktopAppModel
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.sys.window.render.AppLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeleteAlert(
  app: DesktopAppModel,
  microModule: NativeMicroModule.NativeRuntime,
  onDismissRequest: () -> Unit,
  onConfirm: () -> Unit,
) {
  BasicAlertDialog(
    onDismissRequest = onDismissRequest
  ) {
    Column(
      Modifier
        .clip(RoundedCornerShape(10))
        .background(Color.White.copy(alpha = 0.8f))
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      AppLogo.from(app.icon, fetchHook = microModule.blobFetchHook)
        .toDeskAppIcon(containerAlpha = 1f).Render()
      when {
        app.isWebLink -> {
          Text("${BrowserI18nResource.Desktop.delete.text}: \"${app.name}\"", color = Color.Black)
        }

        else -> {
          Text(
            "${BrowserI18nResource.Desktop.uninstall.text}: \"${app.name}\"",
            color = Color.Black
          )
          Text(BrowserI18nResource.Desktop.uninstallAlert.text, color = Color.Black)
        }
      }

      Row {
        TextButton(onDismissRequest) {
          Text(
            BrowserI18nResource.button_name_cancel.text,
            color = Color.Black
          )
        }
        Spacer(Modifier.width(50.dp))
        TextButton(onConfirm) {
          Text(
            BrowserI18nResource.button_name_confirm.text,
            color = Color.Red
          )
        }
      }
    }
  }
}