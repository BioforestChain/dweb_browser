package org.dweb_browser.browser.download.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.common.CommonSimpleTopBar
import org.dweb_browser.browser.download.DownloadController
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.noLocalProvidedFor
import org.dweb_browser.helper.formatTimestamp

val LocalDecompressModel = compositionLocalOf<DecompressModel> {
  noLocalProvidedFor("LocalDecompressModel")
}

class DecompressModel(private val downloadController: DownloadController) {
  val downloadTask: MutableState<DownloadTask?> = mutableStateOf(null)
  val showProgress: MutableState<Boolean> = mutableStateOf(false)
  val showError: MutableState<Boolean> = mutableStateOf(false)
  val errMsg: String = ""

  fun show(task: DownloadTask) {
    this.downloadTask.value = task
  }

  fun hide() {
    showError.value = false
    showProgress.value = false
    downloadTask.value = null
  }

  suspend fun showProgress() {
    showError.value = false
    showProgress.value = true
    // 执行解压操作
    downloadTask.value?.let { task ->
      downloadController.decompress(task)
    }
  }

  fun showError(message: String) {
    showProgress.value = false
    showError.value = true
  }

  fun hidePopup() {
    showProgress.value = false
    showError.value = false
  }
}

@Composable
fun DecompressView() {
  val decompressModel = LocalDecompressModel.current
  if (decompressModel.downloadTask.value != null) {
    BackHandler { decompressModel.hide() }
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
      CommonSimpleTopBar(title = BrowserI18nResource.top_bar_title_down_detail()) {
        decompressModel.hide()
      }
      Box {
        AppHeadInfo()
        // BottomButton()
        ProgressView()
        ErrorView()
      }
    }
  }
}

@Composable
private fun AppHeadInfo() {
  LocalDecompressModel.current.downloadTask.value?.let { task ->
    LazyColumn {
      item { TableRow(title = BrowserI18nResource.unzip_title_no(), content = task.id) }
      item { TableRow(title = BrowserI18nResource.unzip_title_url(), content = task.url) }
      item {
        TableRow(
          title = BrowserI18nResource.unzip_title_createTime(),
          content = task.createTime.formatTimestamp()
        )
      }
      item {
        TableRow(
          title = BrowserI18nResource.unzip_title_originMmid(),
          content = task.originMmid
        )
      }
      item {
        TableRow(
          title = BrowserI18nResource.unzip_title_originUrl(),
          content = task.originUrl ?: ""
        )
      }
      item { TableRow(title = BrowserI18nResource.unzip_title_mime(), content = task.mime) }
    }
  }
}

@Composable
private fun TableRow(title: String, content: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 16.dp, end = 16.dp, top = 16.dp)
  ) {
    Text(
      text = title,
      modifier = Modifier.weight(0.3f),
      color = MaterialTheme.colorScheme.outline
    )
    Text(text = content, modifier = Modifier.weight(0.7f))
  }
}

@Composable
fun BoxScope.BottomButton() {
  val decompressModel = LocalDecompressModel.current
  val scope = rememberCoroutineScope()
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .align(Alignment.BottomCenter)
  ) {
    Button(
      onClick = { scope.launch { decompressModel.showProgress() } },
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp)
    ) {
      Text(text = BrowserI18nResource.unzip_button_install())
    }
    Button(
      onClick = { decompressModel.hide() },
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp)
    ) {
      Text(text = BrowserI18nResource.unzip_button_cancel())
    }
  }
}

@Composable
private fun ProgressView() {
  val decompressModel = LocalDecompressModel.current
  if (decompressModel.showProgress.value) {
    Box(modifier = Modifier
      .fillMaxSize()
      .clickableWithNoEffect { }
      .background(MaterialTheme.colorScheme.outlineVariant.copy(0.5f)),
      contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
  }
}

@Composable
private fun ErrorView() {
  val decompressModel = LocalDecompressModel.current
  if (decompressModel.showError.value) {
    Box(modifier = Modifier
      .fillMaxSize()
      .clickableWithNoEffect { }
      .background(MaterialTheme.colorScheme.outlineVariant.copy(0.5f))) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .background(MaterialTheme.colorScheme.background)
      ) {
        Text(text = decompressModel.errMsg)
      }
    }
  }
}