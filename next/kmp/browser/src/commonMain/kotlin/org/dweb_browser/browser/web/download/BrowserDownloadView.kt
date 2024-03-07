package org.dweb_browser.browser.web.download

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.RadioButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.web.debugBrowser
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.button_delete
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.download_page_complete
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.download_page_delete
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.download_page_manage
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_state_init
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_state_install
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_state_open
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_state_pause
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_state_resume
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_tip_cancel
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_tip_continue
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_tip_exist
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_tip_reload
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.tab_downloaded
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.tab_downloading
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.tip_empty
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.formatDatestampByMilliseconds
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.helper.valueIn
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.NativeBackHandler
import org.dweb_browser.sys.window.render.imageFetchHook

@Composable
fun BrowserDownloadModel.BrowserDownloadSheet(downloadItem: BrowserDownloadItem) {
  AnimatedContent(targetState = alreadyExist.value) {
    when (it) {
      true -> {
        BrowserDownloadTip(downloadItem)
      }

      false -> {
        BrowserDownloadView(downloadItem)
      }
    }
  }
}

@Composable
private fun BrowserDownloadModel.BrowserDownloadTip(downloadItem: BrowserDownloadItem) {
  Card(elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp)) {
    Column(
      Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = sheet_download_tip_exist(),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Button(onClick = { close() }) {
          Text(text = sheet_download_tip_cancel())
        }

        Button(onClick = { alreadyExist.value = false }) {
          val text =
            if (downloadItem.state.valueIn(DownloadState.Downloading, DownloadState.Paused)) {
              sheet_download_tip_continue()
            } else sheet_download_tip_reload()
          Text(text = text)
        }
      }
    }
  }
}

@Composable
fun BrowserDownloadModel.BrowserDownloadView(downloadItem: BrowserDownloadItem) {
  val state = LocalWindowController.current.state
  val microModule by state.constants.microModule

  Card(elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp)) {
    Column(
      Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = sheet_download(),
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
          icon = downloadItem.fileSuffix.icon,
          modifier = Modifier.size(56.dp),
          iconFetchHook = microModule?.imageFetchHook
        )

        Column(
          modifier = Modifier.weight(1f).height(56.dp),
          verticalArrangement = Arrangement.SpaceAround
        ) {
          Text(
            text = downloadItem.fileName,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = downloadItem.downloadArgs.contentLength.toSpaceSize(),
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium,
          )
        }

        val progress =
          if (downloadItem.state.state.valueIn(DownloadState.Downloading, DownloadState.Paused)) {
            downloadItem.state.current * 1.0f / downloadItem.state.total
          } else {
            1.0f
          }
        Box(
          modifier = Modifier.clip(RoundedCornerShape(32.dp)).widthIn(min = 90.dp)
            .background(
              brush = Brush.horizontalGradient(
                0.0f to MaterialTheme.colorScheme.primary,
                progress to MaterialTheme.colorScheme.primary,
                progress to MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                1.0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
              )
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickableWithNoEffect { clickDownloadButton(downloadItem) },
          contentAlignment = Alignment.Center
        ) {
          Text(text = ButtonText(downloadItem), color = MaterialTheme.colorScheme.background)
        }
      }
    }
  }
}

@Composable
private fun ButtonText(downloadItem: BrowserDownloadItem, showProgress: Boolean = true): String {
  return when (downloadItem.state.state) {
    DownloadState.Init, DownloadState.Canceled, DownloadState.Failed -> {
      sheet_download_state_init()
    }

    DownloadState.Downloading -> {
      if (showProgress) {
        val progress = (downloadItem.state.current * 1000 / downloadItem.state.total) / 10.0f
        "$progress %"
      } else {
        sheet_download_state_pause()
      }
    } // 显示百分比
    DownloadState.Paused -> sheet_download_state_resume()
    DownloadState.Completed -> {
      if (downloadItem.fileSuffix.type == BrowserDownloadType.Application)
        sheet_download_state_install()
      else
        sheet_download_state_open()
    }
  }
}

// ---------------------------------------------- 分割线 --------------------------------------------

/**
 * 下载管理界面
 */
@Composable
fun BrowserDownloadModel.BrowserDownloadManage(onClose: () -> Unit) {
  AnimatedContent(targetState = managePage.value) { page ->
    when (page) {
      BrowserDownloadManagePage.Manage -> BrowserDownloadManageView(onClose)
      BrowserDownloadManagePage.DeleteAll -> BrowserDownloadDeleteView {
        managePage.value = BrowserDownloadManagePage.Manage
      }

      BrowserDownloadManagePage.MoreCompleted -> BrowserDownloadMoreView {

      }

      BrowserDownloadManagePage.DeleteCompleted -> BrowserDownloadDeleteView {
        managePage.value = BrowserDownloadManagePage.MoreCompleted
      }
    }
  }
}

@Composable
fun BrowserDownloadModel.BrowserDownloadManageView(onClose: () -> Unit) {
  NativeBackHandler { onClose() }
  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
    DownloadTopBar(title = download_page_manage(), onBack = onClose) {
      Image(
        Icons.Default.EditNote,
        contentDescription = "Delete Manage",
        modifier = Modifier.clip(CircleShape).clickable {
          managePage.value = BrowserDownloadManagePage.DeleteAll
        }.size(32.dp).padding(4.dp)
      )
    }

    if (saveDownloadMaps.isEmpty()) {
      DownloadEmptyTask()
      return
    }

    val downloadingList = mutableListOf<BrowserDownloadItem>()
    val downloadedList = mutableListOf<BrowserDownloadItem>()
    saveDownloadMaps.forEach { (key, items) ->
      items.forEach { item ->
        if (item.state.state.valueIn(DownloadState.Completed)) {
          downloadedList.add(item)
        } else {
          downloadingList.add(item)
        }
      }
    }
    downloadingList.sortByDescending { it.downloadTime }
    downloadedList.sortByDescending { it.downloadTime }

    LazyColumn(
      modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.outlineVariant)
    ) {
      if (downloadingList.isNotEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
          ) {
            Text(text = tab_downloading(), modifier = Modifier.padding(8.dp))
            downloadingList.forEach { item ->
              item.RowDownloadItem { /* TODO 点击操作 */ }
            }
          }
        }
      }

      if (downloadedList.isNotEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
          ) {
            Text(text = tab_downloaded(), modifier = Modifier.padding(8.dp))

            downloadedList.forEach { item ->
              item.RowDownloadItem { /* TODO 点击操作 */ }
            }
          }
        }
      }
    }
  }
}

/**
 * 每行显示的下载信息
 */
@Composable
private fun BrowserDownloadItem.RowDownloadItem(onClick: () -> Unit) {
  val windowState = LocalWindowController.current.state
  val microModule by windowState.constants.microModule

  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      AppIcon(
        icon = fileSuffix.icon,
        modifier = Modifier.padding(8.dp).size(56.dp),
        iconFetchHook = microModule?.imageFetchHook
      )

      Column {
        Text(text = fileName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        if (state.state.valueIn(DownloadState.Downloading, DownloadState.Paused)) {
          LinearProgressIndicator(
            progress = { state.current * 1.0f / state.total },
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            trackColor = MaterialTheme.colorScheme.background
          )
          Text(
            text = "${state.current.toSpaceSize()} / ${state.total.toSpaceSize()}",
            fontSize = 12.sp
          )
        } else {
          Text(
            text = "${downloadTime.formatDatestampByMilliseconds()}    ${state.total.toSpaceSize()}",
            fontSize = 12.sp
          )
        }
      }
    }

    Button(
      modifier = Modifier.padding(8.dp).width(80.dp),
      onClick = onClick
    ) {
      Text(
        text = ButtonText(this@RowDownloadItem, false),
        maxLines = 1
      )
    }
  }
}

@Composable
private fun BrowserDownloadModel.BrowserDownloadDeleteView(onBack: () -> Unit) {
  val selected = remember { mutableStateOf(false) }
  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
    DownloadTopBar(title = download_page_delete(), onBack = onBack) {
      RadioButton(selected = selected.value, onClick = { selected.value = !selected.value })
    }

    Box(modifier = Modifier.fillMaxSize()) {
      if (saveDownloadMaps.isNotEmpty()) {
        val list = mutableListOf<BrowserDownloadItem>()
        saveDownloadMaps.values.forEach { items -> list.addAll(items) }
        list.sortByDescending { item -> item.downloadTime }
        val selectStateMap = mutableMapOf<Long, MutableState<Boolean>>()
        list.forEach { selectStateMap[it.downloadTime] = mutableStateOf(false) }

        LaunchedEffect(selectStateMap) {
          snapshotFlow { selected.value }.collect { s1 ->
            selectStateMap.forEach { (_, s2) -> s2.value = s1 }
          }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = 54.dp)) {
          items(list) { item ->
            key(item, selected) {
              item.RowDownloadItemDelete(selectStateMap[item.downloadTime]!!)
            }
          }
        }
      } else {
        DownloadEmptyTask()
      }

      Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
          .padding(vertical = 8.dp).height(38.dp).align(Alignment.BottomCenter),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Image(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete",
          modifier = Modifier.size(20.dp)
        )
        Text(text = button_delete(), fontSize = 12.sp)
      }
    }
  }
}

/**
 * 每行显示的下载信息
 */
@Composable
private fun BrowserDownloadItem.RowDownloadItemDelete(select: MutableState<Boolean>) {
  val windowState = LocalWindowController.current.state
  val microModule by windowState.constants.microModule

  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      AppIcon(
        icon = fileSuffix.icon,
        modifier = Modifier.padding(8.dp).size(56.dp),
        iconFetchHook = microModule?.imageFetchHook
      )

      Column {
        Text(text = fileName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        if (state.state.valueIn(DownloadState.Downloading, DownloadState.Paused)) {
          LinearProgressIndicator(
            progress = { state.current * 1.0f / state.total },
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            trackColor = MaterialTheme.colorScheme.background
          )
          Text(
            text = "${state.current.toSpaceSize()} / ${state.total.toSpaceSize()}",
            fontSize = 12.sp
          )
        } else {
          Text(
            text = "${downloadTime.formatDatestampByMilliseconds()}    ${state.total.toSpaceSize()}",
            fontSize = 12.sp
          )
        }
      }
    }

    RadioButton(
      modifier = Modifier.padding(end = 8.dp),
      selected = select.value,
      onClick = { select.value = !select.value }
    )
  }
}

@Composable
private fun BrowserDownloadModel.BrowserDownloadMoreView(onBack: () -> Unit) {
  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
    DownloadTopBar(title = download_page_complete(), onBack = onBack) {
      Image(
        imageVector = Icons.Default.EditNote,
        contentDescription = "Delete Manage",
        modifier = Modifier.clip(CircleShape).clickable {
          managePage.value = BrowserDownloadManagePage.DeleteCompleted
        }.size(32.dp).padding(4.dp)
      )
    }
  }
}

@Composable
private fun DownloadTopBar(
  title: String, onBack: () -> Unit, action: (@Composable RowScope.() -> Unit)? = null
) {
  Row(
    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 8.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Image(
        imageVector = Icons.Default.ArrowBackIosNew,
        contentDescription = "Back",
        modifier = Modifier.clip(CircleShape).clickable { onBack() }.size(32.dp).padding(4.dp)
      )

      Text(text = title, modifier = Modifier.padding(horizontal = 16.dp))
    }

    action?.let { action() }
  }
}

@Composable
private fun DownloadEmptyTask() {
  Card(
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier.fillMaxWidth().padding(8.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
        .padding(vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Image(imageVector = Icons.Default.Download, contentDescription = "Empty Download")
      Spacer(modifier = Modifier.height(16.dp))
      Text(text = tip_empty())
    }
  }
}