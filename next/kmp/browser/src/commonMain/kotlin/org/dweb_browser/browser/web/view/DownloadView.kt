package org.dweb_browser.browser.web.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.ktor.http.Url
import org.dweb_browser.browser.download.ui.lastPath
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.helper.decodeURI
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.imageFetchHook

private val mapDownloadIcons: MutableMap<String, String> = mutableMapOf<String, String>().apply {
  put("zip", "file:///sys/download/package.svg")
  put("gz", "file:///sys/download/package.svg")
  put("rar", "file:///sys/download/package.svg")
  put("apk", "file:///sys/download/android.svg")
  put("ios", "file:///sys/download/ios.svg")
  put("ios", "file:///sys/download/ios.svg")
  put("ios", "file:///sys/download/ios.svg")
  put("ios", "file:///sys/download/ios.svg")
  put("ios", "file:///sys/download/ios.svg")
  put("ios", "file:///sys/download/ios.svg")
  put("ios", "file:///sys/download/ios.svg")
  put("ios", "file:///sys/download/ios.svg")
  put("ios", "file:///sys/download/ios.svg")
  put("doc", "file:///sys/download/word.svg")
  put("docx", "file:///sys/download/word.svg")
  put("xls", "file:///sys/download/excel.svg")
  put("xlsx", "file:///sys/download/excel.svg")
  put("ppt", "file:///sys/download/powerpoint.svg")
  put("pptx", "file:///sys/download/powerpoint.svg")
  put("pptx", "file:///sys/download/powerpoint.svg")
}

private fun getIconPath(mime: String) = mapDownloadIcons[mime] ?: "file:///sys/download/file.svg"

@Composable
fun BrowserDownloadView(args: WebDownloadArgs) {
  val state = LocalWindowController.current.state
  val microModule by state.constants.microModule
  val fileName = args.contentDisposition.substringAfter("filename=").ifEmpty {
    Url(args.url).encodedPath.lastPath().decodeURI()
  }
  val mimeType = fileName.split(".").last() // 这个判断使用文件名称的后缀来判断。

  Card(elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp)) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Text(
        text = "Download File",
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        AppIcon(
          icon = getIconPath(mimeType),
          modifier = Modifier.size(56.dp),
          iconFetchHook = microModule?.imageFetchHook
        )

        Column(
          modifier = Modifier.weight(1f).height(56.dp),
          verticalArrangement = Arrangement.SpaceAround
        ) {
          Text(
            text = fileName,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = args.contentLength.toSpaceSize(),
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium,
          )
        }

        Button(onClick = {}) {
          Text("Download")
        }
      }
    }
  }
}