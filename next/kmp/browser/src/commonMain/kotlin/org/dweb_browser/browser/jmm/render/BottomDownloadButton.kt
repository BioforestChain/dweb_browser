package org.dweb_browser.browser.jmm.render

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.JmmStatus
import org.dweb_browser.browser.jmm.LocalJmmDetailController
import org.dweb_browser.helper.compose.AutoResizeTextContainer
import org.dweb_browser.helper.compose.iosTween
import org.dweb_browser.helper.withScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BoxScope.BottomDownloadButton() {
  val background = MaterialTheme.colorScheme.surface
  val jmmInstallerController = LocalJmmDetailController.current
  val uiScope = rememberCoroutineScope()
  val jmmUiKit = rememberJmmUiKit(jmmInstallerController);

  Box(
    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(
      brush = Brush.verticalGradient(listOf(background.copy(0f), background))
    ).padding(16.dp), contentAlignment = Alignment.Center
  ) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    TooltipBox(
      enableUserInput = false,
      positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
      tooltip = {
        when (jmmUiKit.jmmStatus) {
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
      },
      state = tooltipState,
    ) {
      val bgColor = when {
        jmmUiKit.jmmStatus == JmmStatus.VersionLow -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
      }
      ElevatedButton(
        onClick = {
          when (jmmUiKit.metadata.state.state) {
            JmmStatus.VersionLow -> {
              uiScope.launch {
                tooltipState.show()
              }
            }

            else -> {}
          }
          jmmUiKit.onClickDownloadButton()
        },
        modifier = Modifier.requiredSize(height = 50.dp, width = 300.dp).fillMaxWidth(),
        colors = ButtonDefaults.elevatedButtonColors(
          containerColor = bgColor,
          contentColor = when (jmmUiKit.jmmStatus) {
            JmmStatus.VersionLow -> MaterialTheme.colorScheme.onSecondary
            else -> MaterialTheme.colorScheme.onPrimary
          },
          disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
          disabledContentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        contentPadding = PaddingValues(0.dp),
      ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          /// 进度背景
          if (jmmUiKit.showLinearProgress) {
            LinearProgressIndicator(
              progress = { jmmUiKit.jmmStatusEvent.progress },
              modifier = Modifier.fillMaxSize().alpha(0.5f).zIndex(1f),
              color = bgColor,
            )
          }
          Column(
            Modifier.fillMaxSize().padding(ButtonDefaults.ContentPadding).zIndex(2f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            when (val labelEnd = jmmUiKit.labelEnd) {
              null -> Text(
                text = jmmUiKit.labelStart,
                textAlign = TextAlign.Center,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Visible
              )

              else -> Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                AutoResizeTextContainer(Modifier.weight(1f)) {
                  Text(
                    text = jmmUiKit.labelStart,
                    textAlign = TextAlign.Center,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                  )
                }
                Box(Modifier.fillMaxSize().weight(2f), contentAlignment = Alignment.CenterEnd) {
                  AnimatedCounterText(text = labelEnd)
                }
              }
            }
            when (val description = jmmUiKit.description) {
              null -> {}
              else -> Text(
                text = description,
                modifier = Modifier.weight(0.5f),
                style = MaterialTheme.typography.bodySmall
              )
            }
          }
        }
      }
    }
  }
}


@Composable
fun AnimatedCounterText(
  text: String, modifier: Modifier = Modifier, textStyle: TextStyle? = null,
) {
  Row(modifier) {
    val style = textStyle ?: LocalTextStyle.current
    var preTextEndIndex = 0;
    for (matchResult in Regex("[\\d\\.]+").findAll(text)) {
      if (preTextEndIndex < matchResult.range.first) {
        Text(text.substring(preTextEndIndex, matchResult.range.first), style = style)
      }
      AnimatedCounter(count = matchResult.value, textStyle = style)
      preTextEndIndex = matchResult.range.last + 1
    }
    if (preTextEndIndex < text.length - 1) {
      Text(text.substring(preTextEndIndex), style = style)
    }
  }
}

@Composable
fun AnimatedCounter(count: String, textStyle: TextStyle? = null) {
  Row(
    modifier = Modifier.animateContentSize(),
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val style = textStyle ?: LocalTextStyle.current
    val countNum = count.toFloat()
    count.mapIndexed { index, c -> Digit(c, countNum, index) }.forEach { digit ->
      val animationSpec: FiniteAnimationSpec<IntOffset> =
        iosTween(durationMillis = 100 + digit.place * 50)
      key(count.length - digit.place) {
        AnimatedContent(targetState = digit, transitionSpec = {
          if (targetState > initialState) {
            slideInVertically(animationSpec) { -it } togetherWith slideOutVertically(animationSpec) { it }
          } else {
            slideInVertically(animationSpec) { it } togetherWith slideOutVertically(animationSpec) { -it }
          }
        }) { digit ->
          Text("${digit.digitChar}", style = style)
        }
      }
    }
  }
}

data class Digit(val digitChar: Char, val fullNumber: Float, val place: Int) {
  override fun equals(other: Any?): Boolean {
    return when (other) {
      is Digit -> digitChar == other.digitChar
      else -> super.equals(other)
    }
  }
}

operator fun Digit.compareTo(other: Digit): Int {
  return fullNumber.compareTo(other.fullNumber)
}