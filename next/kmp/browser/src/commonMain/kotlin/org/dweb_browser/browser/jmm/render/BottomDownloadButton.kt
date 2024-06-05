package org.dweb_browser.browser.jmm.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.JmmStatus
import org.dweb_browser.browser.jmm.JmmStatusEvent
import org.dweb_browser.browser.jmm.JsMicroModule
import org.dweb_browser.browser.jmm.LocalJmmInstallerController
import org.dweb_browser.helper.compose.AutoResizeTextContainer
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.helper.withScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BoxScope.BottomDownloadButton() {
  val background = MaterialTheme.colorScheme.surface
  val jmmInstallerController = LocalJmmInstallerController.current
  val jmmState = jmmInstallerController.installMetadata.state
  // 应用是否是当前支持的大版本
  val canSupportTarget =
    jmmInstallerController.installMetadata.metadata.canSupportTarget(JsMicroModule.VERSION)
  val uiScope = rememberCoroutineScope()

  Box(
    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(
      brush = Brush.verticalGradient(listOf(background.copy(0f), background))
    ).padding(16.dp), contentAlignment = Alignment.Center
  ) {
    val showLinearProgress =
      jmmState.state == JmmStatus.Downloading || jmmState.state == JmmStatus.Paused

//    val m2 = if (showLinearProgress) {
//      val percent = jmmState.progress()
//      val color1 = MaterialTheme.colorScheme.primary
//      val color2 = color1.copy(alpha = 0.5f)
//      modifier.background(
//        Brush.horizontalGradient(
//          0.0f to color1,
//          maxOf(percent - 0.01f, 0.0f) to color1,
//          minOf(percent + 0.01f, 1.0f) to color2,
//          1.0f to color2
//        )
//      )
//    } else {
//      modifier.background(MaterialTheme.colorScheme.primary)
//    }

    val tooltipState = rememberTooltipState(isPersistent = true)
    TooltipBox(
      positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(), tooltip = {
        when (jmmState.state) {
          /// 版本偏低时，提示用户，是否要进行降级安装
          JmmStatus.VersionLow -> RichTooltip(title = { Text(BrowserI18nResource.install_tooltip_warning()) },
            action = {
              Row {
                TextButton(onClick = { uiScope.launch { tooltipState.dismiss() } }) {
                  Text(BrowserI18nResource.button_name_cancel())
                }
                ElevatedButton(onClick = {
                  jmmInstallerController.jmmNMM.scopeLaunch(cancelable = true) {
                    withScope(uiScope) {
                      tooltipState.dismiss()
                    }
                    // 版本偏低时，提示用户，是否要进行降级安装
                    jmmInstallerController.createAndStartDownload()
                  }
                }) {
                  Text(BrowserI18nResource.install_tooltip_install_lower_action())
                }
              }
            }) {
            Text(BrowserI18nResource.install_tooltip_lower_version_tip())
          }

          else -> {}
        }
      }, state = tooltipState
    ) {

      val bgColor = when {
        jmmState.state == JmmStatus.VersionLow -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
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
              JmmStatus.VersionLow -> {
                withScope(uiScope) {
                  tooltipState.show()
                }
              }

              JmmStatus.INSTALLED -> {
                jmmInstallerController.openApp()
              }
            }
          }
        },
        modifier = Modifier.requiredSize(height = 50.dp, width = 300.dp).fillMaxWidth(),
        colors = ButtonDefaults.elevatedButtonColors(
          containerColor = bgColor,
          contentColor = when (jmmState.state) {
            JmmStatus.VersionLow -> MaterialTheme.colorScheme.onSecondary
            else -> MaterialTheme.colorScheme.onPrimary
          },
          disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
          disabledContentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        contentPadding = PaddingValues(0.dp),
        enabled = canSupportTarget
      ) {
        Box(Modifier.fillMaxSize()) {
          if (showLinearProgress) {
            LinearProgressIndicator(
              progress = { jmmState.progress },
              modifier = Modifier.fillMaxSize().alpha(0.5f).zIndex(1f),
              color = bgColor,
            )
          }
          Box(
            Modifier.fillMaxSize().padding(ButtonDefaults.ContentPadding).zIndex(2f),
            contentAlignment = Alignment.Center
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
                      text = progressText, modifier = Modifier.weight(2f), textAlign = TextAlign.End
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