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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.JmmStatus
import org.dweb_browser.browser.jmm.JmmStatusEvent
import org.dweb_browser.browser.jmm.JsMicroModule
import org.dweb_browser.browser.jmm.LocalJmmInstallerController
import org.dweb_browser.helper.compose.AutoResizeTextContainer
import org.dweb_browser.helper.toSpaceSize

@Composable
internal fun BoxScope.BottomDownloadButton() {
  val background = MaterialTheme.colorScheme.surface
  val jmmInstallerController = LocalJmmInstallerController.current
  val jmmState = jmmInstallerController.installMetadata.state
  // 应用是否是当前支持的大版本
  val canSupportTarget =
    jmmInstallerController.installMetadata.manifest.canSupportTarget(JsMicroModule.VERSION)

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
      val percent = jmmState.progress()
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
        jmmInstallerController.jmmNMM.scopeLaunch(cancelable = true) {
          when (jmmState.state) {
            JmmStatus.Init, JmmStatus.Failed, JmmStatus.Canceled -> {
              jmmInstallerController.createAndStartDownload()
            }

            JmmStatus.NewVersion -> {
              jmmInstallerController.closeApp()
              jmmInstallerController.createAndStartDownload()
            }

            JmmStatus.Paused -> {
              jmmInstallerController.startDownload()
            }

            JmmStatus.Downloading -> {
              jmmInstallerController.pause()
            }

            JmmStatus.Completed -> {}
            JmmStatus.VersionLow -> {} // 版本偏低时，不响应按键
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
      enabled = canSupportTarget && jmmState.state != JmmStatus.VersionLow // 版本太低，按键置灰
    ) {
      if (canSupportTarget) {
        JmmStatusText(jmmState) { statusName, progressText ->
          progressText?.let {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              AutoResizeTextContainer(Modifier.weight(1f)) {
                Text(
                  text = statusName,
                  textAlign = TextAlign.Center,
                  softWrap = false,
                  maxLines = 1,
                  overflow = TextOverflow.Visible
                )
              }
              Text(
                text = progressText,
                modifier = Modifier.weight(2f),
                textAlign = TextAlign.End
              )
            }
          } ?: Text(text = statusName)
        }
      } else {
        Text(text = BrowserI18nResource.install_button_incompatible())
      }
    }
  }
}

private val JmmStatusEvent.progressText: String?
  get() {
    var text = ""
    if (current > 0) {
      text += current.toSpaceSize()
    }
    if (total > 1 && total > current) {
      if (text.isNotEmpty()) {
        text += " / "
      }
      text += total.toSpaceSize()
    }
    return text.trim().ifEmpty { null } // 如果字符串是空的，直接返回 null
  }

/**
 * 通过 JmmStatusEvent，返回需要显示的状态和文件大小或者进度值
 */
@Composable
fun JmmStatusText(state: JmmStatusEvent, content: @Composable (String, String?) -> Unit) {
  return when (state.state) {
    JmmStatus.Init, JmmStatus.Canceled -> content(
      BrowserI18nResource.install_button_install(), state.progressText
    )

    JmmStatus.NewVersion -> content(
      BrowserI18nResource.install_button_update(), state.progressText
    )

    JmmStatus.Downloading -> content(
      BrowserI18nResource.install_button_downloading(), state.progressText,
    )

    JmmStatus.Paused -> content(
      BrowserI18nResource.install_button_paused(), state.progressText,
    )

    JmmStatus.Completed -> content(
      BrowserI18nResource.install_button_installing(), null
    )

    JmmStatus.INSTALLED -> content(
      BrowserI18nResource.install_button_open(), null
    )

    JmmStatus.Failed -> content(
      BrowserI18nResource.install_button_retry(), null
    )

    JmmStatus.VersionLow -> content(
      BrowserI18nResource.install_button_lower(), null
    )
  }
}