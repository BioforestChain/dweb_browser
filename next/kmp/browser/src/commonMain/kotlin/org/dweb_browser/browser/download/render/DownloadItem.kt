package org.dweb_browser.browser.download.render

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.download.model.LocalDownloadModel
import org.dweb_browser.browser.download.model.getIconByMime
import org.dweb_browser.helper.compose.CommonI18n
import org.dweb_browser.helper.compose.SwipeToViewBox
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.toSpaceSize
import kotlin.math.roundToInt

@Composable
fun MiddleEllipsisText(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  fontSize: TextUnit = TextUnit.Unspecified,
  fontStyle: FontStyle? = null,
  fontWeight: FontWeight? = null,
  fontFamily: FontFamily = FontFamily.Default,
  letterSpacing: TextUnit = TextUnit.Unspecified,
  textDecoration: TextDecoration? = null,
  textAlign: TextAlign = TextAlign.Start,
  lineHeight: TextUnit = TextUnit.Unspecified,
) {
  val textMeasure = rememberTextMeasurer()
  var measuredText by remember { mutableStateOf(text) }
  val textStyle by remember {
    mutableStateOf(
      TextStyle(
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight
      )
    )
  }

  Text(text = measuredText,
    modifier = modifier,
    overflow = TextOverflow.Ellipsis,
    maxLines = 1,
    style = textStyle,
    onTextLayout = { layoutResult ->
      val isLineEllipsized = layoutResult.isLineEllipsized(0)
      val measureWidth = textMeasure.measure(measuredText, textStyle).size.width
      if (isLineEllipsized && measureWidth > layoutResult.size.width) {
        val fontRadio = measuredText.length * 1.0f / measureWidth
        val maxLength = (layoutResult.size.width * fontRadio).roundToInt() - 2
        measuredText = text.take(maxLength / 2) + "...." + text.takeLast(maxLength / 2)
      }
    })
}

fun String.lastPath() = this.substring(this.lastIndexOf("/") + 1)

@Composable
fun DownloadItem(onClick: () -> Unit, onRemove: () -> Unit, downloadTask: DownloadTask) {
  val viewModel = LocalDownloadModel.current
  val scope = rememberCoroutineScope()
  var taskCurrent by remember { mutableLongStateOf(downloadTask.status.current) }
  var taskState by remember { mutableStateOf(downloadTask.status.state) }

  LaunchedEffect(downloadTask) { // 监听状态，更新显示
    if (downloadTask.status.state != DownloadState.Completed) {
      downloadTask.onChange.collect {
        taskState = downloadTask.status.state
        taskCurrent = downloadTask.status.current
      }
    }
  }

  SwipeToViewBox(
    backgroundContent = {
      TextButton(
        onClick = onRemove,
        modifier = Modifier.fillMaxHeight(),
        colors = ButtonDefaults.textButtonColors(
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.error
        ),
        shape = RectangleShape,
      ) {
        Text(CommonI18n.delete(), Modifier.padding(8.dp))
      }
    }
  ) {
    ListItem(
      modifier = Modifier.clickableWithNoEffect { onClick() },
      headlineContent = { // 主标题
        MiddleEllipsisText(text = downloadTask.url.lastPath())
      },
      supportingContent = { // 副标题
        Column {
          when (downloadTask.status.state) {
            DownloadState.Completed -> {
              Row {
                Text(
                  text = downloadTask.status.total.toSpaceSize(),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = downloadTask.status.state.name,
                  textAlign = TextAlign.End,
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }

            else -> {
              // 显示下载进度，右边显示下载状态
              Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = "${taskCurrent.toSpaceSize()} / ${downloadTask.status.total.toSpaceSize()}",
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = taskState.name,
                  textAlign = TextAlign.End,
                  modifier = Modifier.fillMaxWidth()
                )
              }
              // 显示下载进度
              LinearProgressIndicator(progress = { taskCurrent / downloadTask.status.total.toFloat() })
            }
          }
        }
      },
      leadingContent = { // 左边的图标
        Icon(imageVector = getIconByMime(downloadTask.mime), contentDescription = "Download File")
      },
      trailingContent = { // 右边的图标
        Row {
          when (downloadTask.status.state) {
            DownloadState.Downloading -> {
              Image(imageVector = Icons.Default.PlayCircle,
                contentDescription = "Downloading",
                modifier = Modifier.clickableWithNoEffect {
                  scope.launch {
                    when (downloadTask.status.state) {
                      DownloadState.Failed, DownloadState.Paused -> {
                        viewModel.startDownload(downloadTask)
                      }

                      DownloadState.Downloading -> {
                        viewModel.pauseDownload(downloadTask)
                      }

                      else -> {}
                    }
                  }
                })
            }

            DownloadState.Paused -> {
              Image(imageVector = Icons.Default.PauseCircle,
                contentDescription = "Pause",
                modifier = Modifier.clickableWithNoEffect {
                  scope.launch {
                    when (downloadTask.status.state) {
                      DownloadState.Failed, DownloadState.Paused -> {
                        viewModel.startDownload(downloadTask)
                      }

                      DownloadState.Downloading -> {
                        viewModel.pauseDownload(downloadTask)
                      }

                      else -> {}
                    }
                  }
                })
            }

            DownloadState.Completed -> {
              Image(imageVector = Icons.Default.FileOpen,
                contentDescription = "Open",
                modifier = Modifier.clickableWithNoEffect {
                  // TODO 这个后续要做成打开应用功能
                })
            }

            else -> {
              Image(imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                modifier = Modifier.clickableWithNoEffect {
                  scope.launch {
                    when (downloadTask.status.state) {
                      DownloadState.Failed, DownloadState.Paused -> {
                        viewModel.startDownload(downloadTask)
                      }

                      DownloadState.Downloading -> {
                        viewModel.pauseDownload(downloadTask)
                      }

                      else -> {}
                    }
                  }
                })
            }
          }
        }
      },
    )
  }
}