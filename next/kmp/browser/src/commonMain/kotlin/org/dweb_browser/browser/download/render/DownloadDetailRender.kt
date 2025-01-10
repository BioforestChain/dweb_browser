package org.dweb_browser.browser.download.render

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import org.dweb_browser.browser.desk.render.toDeskAppIcon
import org.dweb_browser.browser.download.DownloadI18n
import org.dweb_browser.browser.download.DownloadNMM
import org.dweb_browser.browser.download.model.DecompressModel
import org.dweb_browser.browser.download.model.DownloadTask
import org.dweb_browser.browser.web.data.formatToStickyName
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.formatTimestampByMilliseconds
import org.dweb_browser.helper.platform.theme.LocalColorful
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.sys.toast.ext.showToast
import org.dweb_browser.sys.window.core.constant.LocalWindowMM
import org.dweb_browser.sys.window.render.AppLogo

@Composable
fun DecompressModel.Render(downloadTask: DownloadTask, modifier: Modifier) {
  Box(modifier) {
    downloadTask.AppHeadInfo()
    showProgress.trueAlso {
      ProgressView()
    }
    showError.trueAlso {
      ErrorView()
    }
  }
}

@Composable
private fun DownloadTask.AppHeadInfo(modifier: Modifier = Modifier) {
  val mm = LocalWindowMM.current as DownloadNMM.DownloadRuntime
  LazyColumn(
    modifier.padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item { TableRow(title = DownloadI18n.unzip_label_no(), content = id) }
    item { TableRow(title = DownloadI18n.unzip_label_name(), filepath.toPath().name) }
    item { TableRow(title = DownloadI18n.unzip_label_mime(), content = mime) }
    item {
      TableRow(title = DownloadI18n.unzip_label_url()) { modifier, style ->
        val clipboardManager = LocalClipboardManager.current
        val scope = rememberCoroutineScope()
        val urlCopySuccess = DownloadI18n.url_copy_success()
        Text(
          url, modifier = modifier.clickable {
            clipboardManager.setText(AnnotatedString(url))
            scope.launch { mm.showToast(urlCopySuccess) }
          }, style = style.merge(
            color = LocalColorful.current.Blue.current, textDecoration = TextDecoration.Underline
          )
        )
      }
    }
    item { TableRow(title = DownloadI18n.unzip_label_path(), content = filepath) }
    item {
      TableRow(
        title = DownloadI18n.unzip_label_createTime(),
        content = createTime.formatTimestampByMilliseconds()
      )
      createTime.formatToStickyName()
    }
    item {
      when (val microModule = produceState<MicroModule?>(null) {
        value = mm.bootstrapContext.dns.query(originMmid)
      }.value) {
        null -> TableRow(
          title = DownloadI18n.unzip_label_originMmid(), content = originMmid
        )

        else -> TableRow(
          title = DownloadI18n.unzip_label_originMmid(),
        ) { modifier, style ->
          Row(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
          ) {
            AppLogo.fromResources(microModule.icons, fetchHook = mm.blobFetchHook).toDeskAppIcon()
              .Render(Modifier.size(32.dp))
            Column(
              Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              Text(
                microModule.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                originMmid,
                style = MaterialTheme.typography.bodySmall.run { copy(fontSize = fontSize * 0.8f) },
                fontStyle = FontStyle.Italic
              )
            }
          }
        }
      }

    }
    item {
      TableRow(DownloadI18n.unzip_label_originUrl()) { modifier, style ->
        when (val url = originUrl) {
          null -> Text(
            DownloadI18n.unknown_origin(),
            modifier = modifier.alpha(0.5f),
            style = style,
            fontStyle = FontStyle.Italic
          )

          else -> Text(url, modifier = modifier, style = style)
        }
      }
    }
  }
}

@Composable
private fun TableRow(title: String, content: String) {
  TableRow(title) { modifier, style ->
    Text(content, modifier = modifier, style = style)
  }
}

@Composable
private fun TableRow(title: String, content: AnnotatedString) {
  TableRow(title) { modifier, style ->
    Text(content, modifier = modifier, style = style)
  }
}

@Composable
private fun TableRow(title: String, content: @Composable (Modifier, TextStyle) -> Unit) {
  Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
    Text(
      title,
      modifier = Modifier.weight(0.3f).alpha(0.8f),
      style = MaterialTheme.typography.labelMedium
    )
    content(Modifier.weight(0.7f), MaterialTheme.typography.bodyMedium)
  }
  HorizontalDivider()
}

@Composable
private fun DecompressModel.ProgressView() {
  Box(modifier = Modifier.fillMaxSize().clickableWithNoEffect { }
    .background(MaterialTheme.colorScheme.outlineVariant.copy(0.5f)),
    contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
}

@Composable
private fun DecompressModel.ErrorView() {
  Box(modifier = Modifier.fillMaxSize().clickableWithNoEffect { }
    .background(MaterialTheme.colorScheme.outlineVariant.copy(0.5f))) {
    Box(
      modifier = Modifier.fillMaxWidth().padding(16.dp)
        .background(MaterialTheme.colorScheme.background)
    ) {
      Text(text = errMsg)
    }
  }
}