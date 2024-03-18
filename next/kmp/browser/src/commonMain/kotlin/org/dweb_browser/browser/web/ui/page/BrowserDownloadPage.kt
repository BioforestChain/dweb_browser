package org.dweb_browser.browser.web.ui.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.web.data.BrowserDownloadItem
import org.dweb_browser.browser.web.data.BrowserDownloadType
import org.dweb_browser.browser.web.model.page.BrowserDownloadPage
import org.dweb_browser.browser.web.model.page.DownloadPage
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.download_page_complete
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.download_page_delete
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.download_page_delete_checked
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.download_page_manage
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.tab_downloaded
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.tab_downloaded_more
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.tab_downloading
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.tip_empty
import org.dweb_browser.browser.web.ui.common.BrowserTopBar
import org.dweb_browser.helper.compose.AutoResizeTextContainer
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.format
import org.dweb_browser.helper.formatDatestampByMilliseconds
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.helper.valueIn
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.imageFetchHook

@Composable
fun BrowserDownloadPage.BrowserDownloadPageRender(modifier: Modifier) {
  AnimatedContent(
    targetState = downloadPage,
    modifier = modifier,
    transitionSpec = {
      if (targetState.ordinal > initialState.ordinal) {
        (slideInHorizontally { fullWidth -> fullWidth } + fadeIn()).togetherWith(
          slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut())
      } else {
        (slideInHorizontally { fullWidth -> -fullWidth } + fadeIn()).togetherWith(
          slideOutHorizontally { fullWidth -> fullWidth } + fadeOut())
      }
    }
  ) { page ->
    when (page) {
      // 跳转下载管理界面，包含了“下载中”和“已下载”两个列表，“已下载”列表当前只展示最新的五条记录
      DownloadPage.Manage -> BrowserDownloadHomePage(
        openMore = { downloadPage = DownloadPage.MoreCompleted },
        openDelete = { downloadPage = DownloadPage.DeleteAll }
      )
      // 跳转删除下载数据界面，列表内容包含了所有的下载数据
      DownloadPage.DeleteAll -> BrowserDownloadDeletePage(onlyComplete = false) {
        downloadPage = DownloadPage.Manage
      }
      // 跳转已下载数据界面，列表内容包含了所有的“已下载”数据
      DownloadPage.MoreCompleted -> BrowserDownloadMorePage(
        onBack = { downloadPage = DownloadPage.Manage },
        openDelete = { downloadPage = DownloadPage.DeleteCompleted }
      )
      // 跳转删除下载数据界面，列表内容包含了所有的“已下载”数据
      DownloadPage.DeleteCompleted -> BrowserDownloadDeletePage(onlyComplete = true) {
        downloadPage = DownloadPage.MoreCompleted
      }
    }
  }
}

/**
 * 下载管理界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserDownloadPage.BrowserDownloadHomePage(
  openMore: () -> Unit, openDelete: () -> Unit
) {
  Column(modifier = Modifier.fillMaxSize()) {
    BrowserTopBar(
      title = download_page_manage(),
      enableNavigation = false,
      actions = {
        Image(
          Icons.Default.EditNote,
          contentDescription = "Delete Manage",
          modifier = Modifier.clip(CircleShape).clickable { openDelete() }.size(32.dp).padding(4.dp)
        )
      }
    )

    if (saveDownloadList.isEmpty() && saveCompleteList.isEmpty()) {
      DownloadEmptyTask()
      return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      if (saveDownloadList.isNotEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
          ) {
            Text(
              text = tab_downloading(),
              modifier = Modifier.padding(8.dp),
              fontWeight = FontWeight.Bold
            )
            saveDownloadList.forEach { item ->
              item.RowDownloadItem { clickDownloadButton(item) }
            }
          }
        }
      }

      if (saveCompleteList.isNotEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = tab_downloaded(),
                modifier = Modifier.padding(8.dp),
                fontWeight = FontWeight.Bold
              )

              Row(
                modifier = Modifier.clickableWithNoEffect { openMore() },
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = tab_downloaded_more())
                Icon(
                  imageVector = Icons.Default.ArrowBackIosNew,
                  contentDescription = "More",
                  modifier = Modifier.padding(8.dp).size(16.dp).rotate(180f),
                  tint = MaterialTheme.colorScheme.outline
                )
              }
            }

            // 考虑 已下载 可能比较多，这边只显示五个。上面有个点击查看更多的操作。
            saveCompleteList.take(5).forEach { item ->
              item.RowDownloadItem { clickCompleteButton(item) }
            }
          }
        }
      }
    }
  }
}

/**
 * 显示所有的“已下载”数据列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserDownloadPage.BrowserDownloadMorePage(
  onBack: () -> Unit, openDelete: () -> Unit
) {
  LocalWindowController.current.GoBackHandler { onBack() }
  Column(modifier = Modifier.fillMaxSize()) {
    BrowserTopBar(
      title = download_page_complete(),
      onNavigationBack = onBack,
      actions = {
        Image(
          imageVector = Icons.Default.EditNote,
          contentDescription = "Delete Manage",
          modifier = Modifier.clip(CircleShape).clickable { openDelete() }.size(32.dp).padding(4.dp)
        )
      }
    )

    if (saveCompleteList.isEmpty()) {
      DownloadEmptyTask()
      return
    }
    LazyColumn {
      item {
        Card(
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
          saveCompleteList.forEach { item ->
            item.RowDownloadItem { clickCompleteButton(item) }
          }
        }
      }
    }
  }
}

/**
 * 下载信息行
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

    DownloadButton(downloadItem = this@RowDownloadItem, showProgress = false, onClick = onClick)
  }
}

/**
 * 删除下载数据界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserDownloadPage.BrowserDownloadDeletePage(
  onlyComplete: Boolean = true, onBack: () -> Unit
) {
  LocalWindowController.current.GoBackHandler { onBack() }
  val selected = remember { mutableStateOf(false) }
  val list = remember(saveDownloadList, saveCompleteList) { // 只有列表变化的时候，这个才会被重组
    if (onlyComplete) saveCompleteList else saveDownloadList + saveCompleteList
  }
  val selectStateMap = remember(list) { // 只有列表变化的时候，这个才会被重组
    list.associateWith { mutableStateOf(false) }
  }
  val size = selectStateMap.values.filter { it.value }.size
  Column(modifier = Modifier.fillMaxSize()) {
    Box(modifier = Modifier.fillMaxWidth()) {
      BrowserTopBar(
        title = if (size == 0) {
          download_page_delete()
        } else {
          download_page_delete_checked().format(size)
        },
        onNavigationBack = onBack,
        actions = {
          Checkbox(
            checked = selected.value,
            onCheckedChange = { check ->
              selected.value = check
              selectStateMap.forEach { it.value.value = check }
            }
          )
          IconButton(enabled = size > 0, onClick = {
            val deleteList = mutableListOf<BrowserDownloadItem>()
            selectStateMap.forEach { (item, value) -> if (value.value) deleteList.add(item) }
            deleteDownloadItems(deleteList)
          }) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete Selects")
          }
        }
      )
    }

    Box(modifier = Modifier.fillMaxSize()) {
      if (selectStateMap.isNotEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = 54.dp)) {
          item {
            Card(
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
              list.forEach { item ->
                key(item, selected) {
                  item.RowDownloadItemDelete(selectStateMap[item]!!) {
                    selected.value = selectStateMap.values.find { !it.value } == null
                  }
                }
              }
            }
          }
        }
      } else {
        DownloadEmptyTask()
      }
    }
  }
}

/**
 * 每行显示的下载信息
 */
@Composable
private fun BrowserDownloadItem.RowDownloadItemDelete(
  select: MutableState<Boolean>, onClick: (Boolean) -> Unit
) {
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

    Checkbox(
      modifier = Modifier.padding(end = 8.dp),
      checked = select.value,
      onCheckedChange = { check ->
        select.value = check
        onClick(check)
      }
    )
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

/**
 * 按钮显示内容
 * 根据showProgress来确认按钮是否要显示进度
 */
@Composable
fun DownloadButton(
  downloadItem: BrowserDownloadItem,
  showProgress: Boolean = true,
  onClick: () -> Unit
) {
  val showText = when (downloadItem.state.state) {
    DownloadState.Init, DownloadState.Canceled, DownloadState.Failed -> {
      BrowserDownloadI18nResource.sheet_download_state_init()
    }
    // 显示百分比
    DownloadState.Downloading -> {
      if (showProgress) {
        val progress = (downloadItem.state.current * 1000 / downloadItem.state.total) / 10.0f
        "$progress %"
      } else {
        BrowserDownloadI18nResource.sheet_download_state_pause()
      }
    }

    DownloadState.Paused -> BrowserDownloadI18nResource.sheet_download_state_resume()
    DownloadState.Completed -> {
      if (downloadItem.fileSuffix.type == BrowserDownloadType.Application)
        BrowserDownloadI18nResource.sheet_download_state_install()
      else
        BrowserDownloadI18nResource.sheet_download_state_open()
    }
  }

  val progress = if (showProgress &&
    downloadItem.state.state.valueIn(DownloadState.Downloading, DownloadState.Paused)
  ) {
    downloadItem.state.current * 1.0f / downloadItem.state.total
  } else {
    1.0f
  }
  Box(
    modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(32.dp)).width(90.dp)
      .background(
        brush = Brush.horizontalGradient(
          0.0f to MaterialTheme.colorScheme.primary,
          progress to MaterialTheme.colorScheme.primary,
          progress to MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
          1.0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        )
      )
      .padding(vertical = 8.dp)
      .clickableWithNoEffect { onClick() },
    contentAlignment = Alignment.Center
  ) {
    AutoResizeTextContainer(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = showText,
        softWrap = false,
        color = MaterialTheme.colorScheme.background,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = Modifier.align(Alignment.Center)
      )
    }
  }
}