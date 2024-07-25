package org.dweb_browser.browser.web.ui.page

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.model.DownloadState
import org.dweb_browser.browser.download.model.DownloadStateEvent
import org.dweb_browser.browser.web.data.BrowserDownloadItem
import org.dweb_browser.browser.web.data.BrowserDownloadType
import org.dweb_browser.browser.web.model.page.BrowserDownloadPage
import org.dweb_browser.browser.web.ui.common.BrowserTopBar
import org.dweb_browser.helper.compose.NoDataRender
import org.dweb_browser.helper.toSpaceSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserDownloadPage.BrowserDownloadPageRender(modifier: Modifier) {
  // 分为顶部的 Chip 和 下面的下载内容列表
  var chipType by remember { mutableStateOf(BrowserDownloadType.All) }
  Column(modifier = modifier) {
    BrowserTopBar(
      title = BrowserI18nResource.Download.page_title_manage(), enableNavigation = false
    )
    TopChipBar { chipType = it }
    ContentDownloadManage(chipType)
  }
}

@Composable
private fun BrowserDownloadPage.TopChipBar(onClick: (chipType: BrowserDownloadType) -> Unit) {
  // 获取列表中的类型，存在至少两个类型，才需要显示当前Chip
  val filterMap = remember(saveDownloadList, saveCompleteList) {
    (saveDownloadList + saveCompleteList).associateBy({ it.fileType }, { false }).toMutableMap()
  }
  if (filterMap.size > 1) {
    filterMap.getOrPut(BrowserDownloadType.All) { true }
    val filterList = filterMap.toList().sortedBy { it.first.ordinal } // 增加all标签
    LazyRow(
      contentPadding = PaddingValues(horizontal = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      items(filterList, key = { it.first }) { item ->
        FilterChip(
          selected = item.second,
          onClick = {
            filterMap.forEach { (key, _) -> filterMap[key] = false } // 所有都置 false
            filterMap[item.first] = true // 当前点击的为 true
            onClick(item.first)
          },
          label = {
            Text(item.first.title())
          },
          leadingIcon = {
            Image(
              painter = item.first.painter(),
              contentDescription = item.first.title(),
              modifier = Modifier.size(24.dp)
            )
          }
        )
      }
    }
  }
}

@Composable
private fun BrowserDownloadPage.ContentDownloadManage(chipType: BrowserDownloadType) {
  if (saveDownloadList.isEmpty() && saveCompleteList.isEmpty()) {
    NoDataRender(BrowserI18nResource.Download.tip_empty())
    return
  }
  val downloadList: List<BrowserDownloadItem>
  val completeList: List<BrowserDownloadItem>
  if (chipType == BrowserDownloadType.All) {
    downloadList = saveDownloadList
    completeList = saveCompleteList
  } else {
    downloadList = saveDownloadList.filter { it.fileType == chipType }
    completeList = saveCompleteList.filter { it.fileType == chipType }
  }
  LazyColumn {
    items(downloadList) { item ->
      DownloadItemManage(item)
    }
    items(completeList) { item ->
      DownloadItemManage(item)
    }
  }
}

@Composable
private fun BrowserDownloadPage.DownloadItemManage(downloadItem: BrowserDownloadItem) {
  ListItem(
    modifier = Modifier.fillMaxWidth().height(56.dp).pointerInput(downloadItem) {
      detectTapGestures(
        onPress = { openFileOnDownload(downloadItem) },
        onTap = { openFileOnDownload(downloadItem) },
        onLongPress = { /** TODO 这边实现 长按 后当前界面改为删除选择模式 */ }
      )
    },
    headlineContent = {
      Column {
        Text(text = downloadItem.fileName, style = MaterialTheme.typography.bodyMedium)
        Text(
          text = "${downloadItem.state.current.toSpaceSize()} / ${downloadItem.state.total.toSpaceSize()} • ${downloadItem.state.state.name}",
          style = MaterialTheme.typography.bodySmall
        )
      }
    },
    leadingContent = {
      if (downloadItem.state.state == DownloadState.Completed) {
        Image(
          painter = downloadItem.fileType.painter(),
          contentDescription = downloadItem.fileType.name,
          modifier = Modifier.size(40.dp)
        )
      } else {
        // 下载进度显示
        DownloadProgressIndicator(downloadItem.state) { clickDownloadButton(downloadItem) }
      }
    },
    trailingContent = {
      if (downloadItem.state.state == DownloadState.Completed) {
        DownloadMoreDropMenu(
          onShare = { shareDownloadItem(downloadItem) },
          onDelete = { deleteDownloadItems(listOf(downloadItem)) }
        )
      } else {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Close"
        )
      }
    }
  )
}

@Composable
private fun DownloadProgressIndicator(event: DownloadStateEvent, onClick: () -> Unit) {
  val primary = MaterialTheme.colorScheme.primary
  Box(
    modifier = Modifier.size(40.dp).clip(CircleShape)
      .background(primary.copy(0.2f)).clickable { onClick() },
    contentAlignment = Alignment.Center
  ) {
    when (event.state) {
      DownloadState.Downloading -> Icon(
        imageVector = Icons.Default.Pause,
        contentDescription = "Downloading",
        modifier = Modifier.size(32.dp)
      )

      DownloadState.Paused -> Icon(
        imageVector = Icons.Default.PlayArrow,
        contentDescription = "Pause",
        modifier = Modifier.size(32.dp)
      )

      else -> Icon(
        imageVector = Icons.Default.FileDownload,
        contentDescription = "download",
        modifier = Modifier.size(32.dp)
      )
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawArc(
        color = primary,
        startAngle = -90f,
        sweepAngle = event.progress() * 360f,
        useCenter = false,
        style = Stroke(width = 8f)
      )
    }
  }
}

/**
 * 下载完成后，右边显示更多按钮
 */
@Composable
private fun DownloadMoreDropMenu(onDelete: () -> Unit, onShare: () -> Unit) {
  var expandMenu by remember { mutableStateOf(false) }
  Box {
    Icon(
      imageVector = Icons.Default.MoreHoriz,
      contentDescription = "More",
      modifier = Modifier.clickable { expandMenu = true }
    )
    DropdownMenu(
      expanded = expandMenu,
      onDismissRequest = { expandMenu = false }
    ) {
      DropdownMenuItem(onClick = onShare) {
        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = BrowserI18nResource.Download.dropdown_share())
      }
//      DropdownMenuItem(onClick = onDelete) {
//        Icon(imageVector = Icons.Default.Replay, contentDescription = "Rename")
//        Spacer(modifier = Modifier.width(8.dp))
//        Text(text = BrowserI18nResource.Download.dropdown_rename())
//      }
      DropdownMenuItem(onClick = onDelete) {
        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = BrowserI18nResource.Download.dropdown_delete())
      }
    }
  }
}