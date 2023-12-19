package org.dweb_browser.browser.jmm.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.JmmStatus
import org.dweb_browser.browser.jmm.JmmStatusEvent
import org.dweb_browser.browser.jmm.LocalJmmInstallerController
import org.dweb_browser.helper.toSpaceSize

@Composable
internal fun BoxScope.BottomDownloadButton() {
  val background = MaterialTheme.colorScheme.surface
  val jmmInstallerController = LocalJmmInstallerController.current
  val jmmState = jmmInstallerController.jmmHistoryMetadata.state

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .align(Alignment.BottomCenter)
      .background(
        brush = Brush.verticalGradient(listOf(background.copy(0f), background))
      )
      .padding(16.dp), contentAlignment = Alignment.Center
  ) {
    val showLinearProgress =
      jmmState.state == JmmStatus.Downloading || jmmState.state == JmmStatus.Paused

    val modifier = Modifier
      .requiredSize(height = 50.dp, width = 300.dp)
      .fillMaxWidth()
      .clip(ButtonDefaults.elevatedShape)
    val m2 = if (showLinearProgress) {
      val percent = if (jmmState.total == 0L) {
        0f
      } else {
        jmmState.current * 1.0f / jmmState.total
      }
      modifier.background(
        Brush.horizontalGradient(
          0.0f to MaterialTheme.colorScheme.primary,
          maxOf(percent - 0.02f, 0.0f) to MaterialTheme.colorScheme.primary,
          minOf(percent + 0.02f, 1.0f) to MaterialTheme.colorScheme.outlineVariant,
          1.0f to MaterialTheme.colorScheme.outlineVariant
        )
      )
    } else {
      modifier.background(MaterialTheme.colorScheme.primary)
    }

    val scope = rememberCoroutineScope()

    ElevatedButton(
      onClick = {
        scope.launch {
          when (jmmState.state) {
            JmmStatus.Init, JmmStatus.Failed, JmmStatus.Canceled, JmmStatus.NewVersion -> {
              jmmInstallerController.createAndStartDownload()
            }

            JmmStatus.Downloading -> {
              jmmInstallerController.pauseDownload()
            }

            JmmStatus.Paused -> {
              jmmInstallerController.startDownload()
            }

            JmmStatus.Completed -> {}
            JmmStatus.INSTALLED -> {
              jmmInstallerController.openApp()
            }
          }
        }
      },
      modifier = m2,
      colors = ButtonDefaults.elevatedButtonColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
        disabledContentColor = MaterialTheme.colorScheme.onErrorContainer,
      ),
    ) {
      val (text, total, current) = JmmStatusText(jmmState, jmmState.current)
      current?.let { size ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Text(text = text, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
          Text(text = size, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
          Text(text = total, modifier = Modifier.weight(1f))
        }
      } ?: Text(text = "$text $total")
    }
  }
}

@Composable
fun JmmStatusText(state: JmmStatusEvent, current: Long): Triple<String, String, String?> {
  return when (state.state) {
    JmmStatus.Init, JmmStatus.Canceled -> Triple(
      first = BrowserI18nResource.install_button_download(),
      second = " " + state.total.toSpaceSize(),
      third = null
    )

    JmmStatus.NewVersion -> Triple(
      first = BrowserI18nResource.install_button_update(),
      second = " " + state.total.toSpaceSize(),
      third = null
    )

    JmmStatus.Downloading -> Triple(
      first = BrowserI18nResource.install_button_downloading(),
      second = " / " + state.total.toSpaceSize(),
      third = current.toSpaceSize()
    )

    JmmStatus.Paused -> Triple(
      first = BrowserI18nResource.install_button_paused(),
      second = " / " + state.total.toSpaceSize(),
      third = current.toSpaceSize()
    )

    JmmStatus.Completed -> Triple(
      first = BrowserI18nResource.install_button_installing(),
      second = "",
      third = null
    )

    JmmStatus.INSTALLED -> Triple(
      first = BrowserI18nResource.install_button_open(),
      second = "",
      third = null
    )

    JmmStatus.Failed -> Triple(
      first = BrowserI18nResource.install_button_retry(),
      second = "",
      third = null
    )
  }
}