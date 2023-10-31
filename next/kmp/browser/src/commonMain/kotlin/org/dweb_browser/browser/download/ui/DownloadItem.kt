package org.dweb_browser.browser.download.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.download.model.DownloadTab
import org.dweb_browser.browser.download.model.getIconByMime
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

  Text(
    text = measuredText,
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
    }
  )
}

fun String.lastPath() = this.substring(this.lastIndexOf("/") + 1)

@Composable
fun DownloadItem(downloadTask: DownloadTask, downloadTab: DownloadTab) {
  val installByteLength = BrowserI18nResource.Companion.InstallByteLength(
    current = downloadTask.status.current, total = downloadTask.status.total
  )
  ListItem(
    headlineContent = { // 主标题
      MiddleEllipsisText(text = downloadTask.url.lastPath())
    },
    supportingContent = { // 副标题
      Column {
        when (downloadTab) {
          DownloadTab.Downloads -> {
            // 显示下载进度，右边显示下载状态
            Row(modifier = Modifier.fillMaxWidth()) {
              Text(
                text = "${downloadTask.status.current.toSpaceSize()} / ${downloadTask.status.total.toSpaceSize()}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = downloadTask.status.state.name,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
              )
            }
            // 显示下载进度
            LinearProgressIndicator(progress = downloadTask.status.current / downloadTask.status.total.toFloat())
          }

          DownloadTab.Files -> {
            Row {
              Text(
                text = downloadTask.status.total.toSpaceSize(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    },
    leadingContent = { // 左边的图标
      Image(imageVector = getIconByMime(downloadTask.mime), contentDescription = "Downloading")
    },
    trailingContent = { // 右边的图标
      Row {
        if (downloadTab == DownloadTab.Downloads) {
          Image(
            imageVector = if (downloadTask.status.state == DownloadState.Paused) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = "State",
            modifier = Modifier.clickableWithNoEffect { }
          )
        }
        Image(
          imageVector = Icons.Default.Menu,
          contentDescription = "Option",
          modifier = Modifier.clickableWithNoEffect { }
        )
      }
    }
  )
}