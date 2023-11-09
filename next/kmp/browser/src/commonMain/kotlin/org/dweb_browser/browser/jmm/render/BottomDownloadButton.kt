package org.dweb_browser.browser.jmm.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.JmmStatus
import org.dweb_browser.browser.jmm.JmmStatusEvent
import org.dweb_browser.browser.jmm.model.LocalJmmViewHelper
import org.dweb_browser.helper.toSpaceSize

@Composable
internal fun BoxScope.BottomDownloadButton() {
  val background = MaterialTheme.colorScheme.surface
  val viewModel = LocalJmmViewHelper.current
  val jmmHistoryMetadata = viewModel.uiState.jmmHistoryMetadata
  var jmmState by remember { mutableStateOf(jmmHistoryMetadata.state.state) }
  var jmmCurrent by remember { mutableLongStateOf(jmmHistoryMetadata.state.current) }
  LaunchedEffect(viewModel.uiState.jmmHistoryMetadata) {
    jmmHistoryMetadata.onJmmStatusChanged {
      jmmState = it.state
      jmmCurrent = it.current
    }
  }

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
      jmmState == JmmStatus.Downloading || jmmState == JmmStatus.Paused

    val modifier = Modifier
      .requiredSize(height = 50.dp, width = 300.dp)
      .fillMaxWidth()
      .clip(ButtonDefaults.elevatedShape)
    val m2 = if (showLinearProgress) {
      val percent = if (jmmHistoryMetadata.state.total == 0L) {
        0f
      } else {
        jmmCurrent * 1.0f / jmmHistoryMetadata.state.total
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

    ElevatedButton(
      onClick = {
        when (jmmState) {
          JmmStatus.Init, JmmStatus.Failed, JmmStatus.Canceled, JmmStatus.NewVersion -> {
            viewModel.startDownload()
          }

          JmmStatus.Downloading -> {
            viewModel.pause()
          }

          JmmStatus.Paused -> {
            viewModel.start()
          }

          JmmStatus.Completed -> {}
          JmmStatus.INSTALLED -> {
            viewModel.open()
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
      Text(
        text = JmmStatusText(jmmHistoryMetadata.state, jmmCurrent)
      )
    }
  }
}

@Composable
fun JmmStatusText(state: JmmStatusEvent, current: Long): String {
  return when (state.state) {
    JmmStatus.Init, JmmStatus.Canceled -> {
      BrowserI18nResource.install_button_download() + " ${state.total.toSpaceSize()}"
    }

    JmmStatus.NewVersion -> {
      BrowserI18nResource.install_button_update() + " ${state.total.toSpaceSize()}"
    }

    JmmStatus.Downloading -> {
      BrowserI18nResource.install_button_downloading() + " ${current.toSpaceSize()} / ${state.total.toSpaceSize()}"
    }

    JmmStatus.Paused -> {
      BrowserI18nResource.install_button_paused() + " ${current.toSpaceSize()} / ${state.total.toSpaceSize()}"
    }

    JmmStatus.Completed -> BrowserI18nResource.install_button_installing()
    JmmStatus.INSTALLED -> BrowserI18nResource.install_button_open()
    JmmStatus.Failed -> BrowserI18nResource.install_button_retry()
  }
}