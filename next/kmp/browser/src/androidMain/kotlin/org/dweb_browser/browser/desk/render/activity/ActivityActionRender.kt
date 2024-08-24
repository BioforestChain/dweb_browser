package org.dweb_browser.browser.desk.render.activity

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.browser.desk.model.ActivityItemRenderProp
import org.dweb_browser.core.std.dns.nativeFetch

@Composable
fun ActivityItem.Action.Render(
  controller: ActivityController, renderProp: ActivityItemRenderProp
) {
  when (val action = this) {
    is ActivityItem.CancelAction -> action.Render(controller, renderProp)
    is ActivityItem.ConfirmAction -> action.Render(controller, renderProp)
    is ActivityItem.LinkAction -> action.Render(controller, renderProp)
  }
}

@Composable
fun ActivityItem.CancelAction.Render(
  controller: ActivityController, renderProp: ActivityItemRenderProp
) {
  FilledTonalButton(
    onClick = {
      uri?.also { uri ->
        controller.deskNMM.scopeLaunch(cancelable = true) {
          controller.deskNMM.nativeFetch(uri)
        }
      }
      renderProp.open = false
    },
    colors = ButtonDefaults.filledTonalButtonColors(
      containerColor = MaterialTheme.colorScheme.errorContainer,
      contentColor = MaterialTheme.colorScheme.error,
    ),
  ) {
    Text(text)
  }
}

@Composable
fun ActivityItem.ConfirmAction.Render(
  controller: ActivityController, renderProp: ActivityItemRenderProp
) {
  FilledTonalButton(
    onClick = {
      uri?.also { uri ->
        controller.deskNMM.scopeLaunch(cancelable = true) {
          controller.deskNMM.nativeFetch(uri)
        }
      }
      renderProp.open = false
    },
    colors = ButtonDefaults.filledTonalButtonColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.primary,
    ),
  ) {
    Text(text)
  }
}

@Composable
fun ActivityItem.LinkAction.Render(
  controller: ActivityController, renderProp: ActivityItemRenderProp
) {
  TextButton(onClick = {
    controller.deskNMM.scopeLaunch(cancelable = true) {
      controller.deskNMM.nativeFetch(uri)
    }
    renderProp.open = false
  }) {
    Text(text)
  }
}
