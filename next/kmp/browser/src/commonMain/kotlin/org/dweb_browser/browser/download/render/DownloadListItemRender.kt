package org.dweb_browser.browser.download.render

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.FileOpen
import androidx.compose.material.icons.twotone.PauseCircle
import androidx.compose.material.icons.twotone.PlayCircle
import androidx.compose.material.icons.twotone.SyncProblem
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
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
import org.dweb_browser.browser.download.DownloadI18n
import org.dweb_browser.browser.download.model.DownloadListModel
import org.dweb_browser.browser.download.model.DownloadState
import org.dweb_browser.browser.download.model.DownloadTask
import org.dweb_browser.helper.compose.CommonI18n
import org.dweb_browser.helper.compose.SwipeToViewBox
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.hoverComposed
import org.dweb_browser.helper.compose.hoverCursor
import org.dweb_browser.helper.compose.rememberSwipeToViewBoxState
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.sys.window.ext.AlertDeleteDialog
import org.dweb_browser.sys.window.ext.FileIconByFilename
import kotlin.math.roundToInt


@Composable
fun DownloadListModel.DownloadItem(
  onClick: () -> Unit,
  onRemove: () -> Unit,
  downloadTask: DownloadTask,
) {
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
  val state = rememberSwipeToViewBoxState()
  val primaryAction: () -> Unit
  val primaryLabel: String
  val primaryIcon: ImageVector
  val primaryDescription: String
  when (downloadTask.status.state) {
    DownloadState.Downloading -> {
      primaryAction = { pauseDownload(downloadTask) }
      primaryLabel = DownloadI18n.pause()
      primaryIcon = Icons.TwoTone.PauseCircle
      primaryDescription = DownloadI18n.downloading()
    }

    DownloadState.Paused -> {
      primaryAction = {
        startDownload(downloadTask)
        state.closeJob()
      }
      primaryLabel = DownloadI18n.resume()
      primaryIcon = Icons.TwoTone.PlayCircle
      primaryDescription = DownloadI18n.paused()
    }

    DownloadState.Completed -> {
      primaryAction = {
        // TODO 这个后续要做成打开应用功能
        state.closeJob()
      }
      primaryLabel = DownloadI18n.open()
      primaryIcon = Icons.TwoTone.FileOpen
      primaryDescription = DownloadI18n.completed()
    }

    DownloadState.Init, DownloadState.Canceled, DownloadState.Failed -> {
      primaryAction = {
        startDownload(downloadTask)
        state.closeJob()
      }
      primaryLabel = DownloadI18n.retry()
      primaryIcon = Icons.TwoTone.SyncProblem
      primaryDescription = DownloadI18n.failed()
    }
  }

  SwipeToViewBox(state = state, backgroundContent = {
    Row {
      val modifier = Modifier.hoverCursor()
      var showDeleteAlert by remember { mutableStateOf(false) }
      TextButton(
        onClick = { showDeleteAlert = true },
        modifier = modifier.fillMaxHeight(),
        colors = ButtonDefaults.textButtonColors(
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.error
        ),
        shape = RectangleShape,
      ) {
        Column(
          Modifier.padding(horizontal = 8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(Icons.TwoTone.Delete, "delete")
          Text(CommonI18n.delete())
        }
      }
      if (showDeleteAlert) {
        AlertDeleteDialog(
          onDismissRequest = { showDeleteAlert = false },
          onDelete = onRemove,
          title = { Text(DownloadI18n.delete_alert_title()) },
          message = {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(DownloadI18n.delete_alert_message { value = downloadTask.filename })
            }
          },
          deleteText = DownloadI18n.confirm_delete()
        )
      }

      TextButton(
        onClick = primaryAction,
        modifier = modifier.fillMaxHeight(),
        colors = ButtonDefaults.textButtonColors(
          containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RectangleShape,
      ) {
        Column(
          Modifier.padding(horizontal = 8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(primaryIcon, primaryDescription)
          Text(primaryLabel)
        }
      }
    }
  }) {
    ListItem(
      modifier = Modifier.clickableWithNoEffect { onClick() },
      headlineContent = { // 主标题
        MiddleEllipsisText(text = downloadTask.filename)
      },
      supportingContent = { // 副标题
        Column {
          when (downloadTask.status.state) {
            DownloadState.Completed -> {
              Text(
                text = downloadTask.status.total.toSpaceSize(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            else -> {
              // 显示下载进度，右边显示下载状态
              Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = "${taskCurrent.toSpaceSize()} / ${downloadTask.status.total.toSpaceSize()}",
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              // 显示下载进度
              LinearProgressIndicator(progress = { taskCurrent / downloadTask.status.total.toFloat() })
            }
          }
        }
      },
      leadingContent = { // 左边的图标
        Box(Modifier.wrapContentSize(), contentAlignment = Alignment.Center) {
          downloadController.downloadNMM.FileIconByFilename(downloadTask.filename, 48.dp)
          when (val downloadState = downloadTask.status.state) {
            DownloadState.Downloading, DownloadState.Paused, DownloadState.Init, DownloadState.Canceled, DownloadState.Failed -> {
              IconButton(
                onClick = primaryAction,
                modifier = Modifier.matchParentSize().hoverCursor()
                  .hoverComposed { hover ->
                    val aniP by animateFloatAsState(
                      when {
                        hover -> 1f
                        else -> 0f
                      }
                    )
                    val alpha = when (downloadState) {
                      DownloadState.Downloading -> aniP
                      else -> aniP * 0.5f + 0.5f
                    }
                    val scale = aniP * 0.5f + 0.4f
                    val offset = 20.dp * (1 - aniP)
                    alpha(alpha).scale(scale).offset(offset, offset)
                  },
                colors = IconButtonDefaults.iconButtonColors(
                  containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  contentColor = MaterialTheme.colorScheme.onPrimary
                ),
              ) {
                Icon(primaryIcon, primaryDescription)
              }
            }

            DownloadState.Completed -> {}
          }
        }
      },
      trailingContent = { // 右边的图标
        IconButton({
          state.toggleJob()
        }, Modifier.hoverCursor()) {
          Icon(Icons.Default.MoreHoriz, "more")
        }
      },
    )
  }
}

@Composable
private fun MiddleEllipsisText(
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


private fun getIconByMime(mime: String): ImageVector {
  return when (mime) {
    "mp4", "avi", "rmvb", "" -> Icons.Default.VideoFile
    "mp3" -> Icons.Default.AudioFile
    "jpg", "png", "bmp", "svg" -> Icons.Default.Photo
    "apk" -> Icons.Default.Android
    else -> Icons.Default.FileDownload
  }
}
