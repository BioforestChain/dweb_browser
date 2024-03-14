package org.dweb_browser.browser.web.download.view

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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.button_delete
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.download_page_complete
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.download_page_delete
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.download_page_delete_checked
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.download_page_manage
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.tab_downloaded
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.tab_downloaded_more
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.tab_downloading
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.tip_empty
import org.dweb_browser.browser.web.download.BrowserDownloadItem
import org.dweb_browser.browser.web.download.BrowserDownloadManagePage
import org.dweb_browser.browser.web.download.BrowserDownloadModel
import org.dweb_browser.helper.compose.HorizontalDivider
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.format
import org.dweb_browser.helper.formatDatestampByMilliseconds
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.helper.valueIn
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.NativeBackHandler
import org.dweb_browser.sys.window.render.imageFetchHook

@Composable
fun BrowserDownloadModel.BrowserDownloadManage(onClose: () -> Unit) {
  AnimatedContent(
    targetState = managePage.value,
    modifier = Modifier.fillMaxSize().clickableWithNoEffect { }, // 避免点击被背后响应了
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
      BrowserDownloadManagePage.Manage -> BrowserDownloadHomePage(
        onClose = onClose,
        openMore = { managePage.value = BrowserDownloadManagePage.MoreCompleted },
        openDelete = { managePage.value = BrowserDownloadManagePage.DeleteAll }
      )
      // 跳转删除下载数据界面，列表内容包含了所有的下载数据
      BrowserDownloadManagePage.DeleteAll -> BrowserDownloadDeletePage(onlyComplete = false) {
        managePage.value = BrowserDownloadManagePage.Manage
      }
      // 跳转已下载数据界面，列表内容包含了所有的“已下载”数据
      BrowserDownloadManagePage.MoreCompleted -> BrowserDownloadMorePage(
        onBack = { managePage.value = BrowserDownloadManagePage.Manage },
        openDelete = { managePage.value = BrowserDownloadManagePage.DeleteCompleted }
      )
      // 跳转删除下载数据界面，列表内容包含了所有的“已下载”数据
      BrowserDownloadManagePage.DeleteCompleted -> BrowserDownloadDeletePage {
        managePage.value = BrowserDownloadManagePage.MoreCompleted
      }
    }
  }
}

/**
 * 下载管理界面
 */
@Composable
private fun BrowserDownloadModel.BrowserDownloadHomePage(
  onClose: () -> Unit, openMore: () -> Unit, openDelete: () -> Unit
) {
  NativeBackHandler { onClose() }
  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
    DownloadTopBar(title = download_page_manage(), onBack = onClose) {
      Image(
        Icons.Default.EditNote,
        contentDescription = "Delete Manage",
        modifier = Modifier.clip(CircleShape).clickable { openDelete() }.size(32.dp).padding(4.dp)
      )
    }

    if (saveDownloadList.isEmpty() && saveCompleteList.isEmpty()) {
      DownloadEmptyTask()
      return
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.outlineVariant)
    ) {
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
@Composable
private fun BrowserDownloadModel.BrowserDownloadMorePage(
  onBack: () -> Unit, openDelete: () -> Unit
) {
  NativeBackHandler { onBack() }
  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
    DownloadTopBar(title = download_page_complete(), onBack = onBack) {
      Image(
        imageVector = Icons.Default.EditNote,
        contentDescription = "Delete Manage",
        modifier = Modifier.clip(CircleShape).clickable { openDelete() }.size(32.dp).padding(4.dp)
      )
    }

    if (saveCompleteList.isEmpty()) {
      DownloadEmptyTask()
      return
    }
    LazyColumn {
      items(saveCompleteList) { item -> item.RowDownloadItem { clickCompleteButton(item) } }
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
@Composable
private fun BrowserDownloadModel.BrowserDownloadDeletePage(
  onlyComplete: Boolean = true, onBack: () -> Unit
) {
  NativeBackHandler { onBack() }
  val selected = remember { mutableStateOf(false) }
  val list = remember(saveDownloadList, saveCompleteList) { // 只有列表变化的时候，这个才会被重组
    if (onlyComplete) saveCompleteList else saveDownloadList + saveCompleteList
  }
  val selectStateMap = remember(list) { // 只有列表变化的时候，这个才会被重组
    list.associateWith { mutableStateOf(false) }
  }
  val size = selectStateMap.values.filter { it.value }.size
  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
    Box(modifier = Modifier.fillMaxWidth()) {
      DownloadTopBar(
        title = if (size == 0) {
          download_page_delete()
        } else {
          download_page_delete_checked().format(size)
        },
        imageVector = Icons.Default.Close,
        onBack = onBack
      ) {
        Checkbox(
          checked = selected.value,
          onCheckedChange = { check ->
            selected.value = check
            selectStateMap.forEach { it.value.value = check }
          }
        )
      }
    }

    Box(modifier = Modifier.fillMaxSize()) {
      if (selectStateMap.isNotEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = 54.dp)) {
          items(list) { item ->
            key(item, selected) {
              item.RowDownloadItemDelete(selectStateMap[item]!!) {
                selected.value = selectStateMap.values.find { !it.value } == null
              }
            }
          }
        }
      } else {
        DownloadEmptyTask()
      }

      BottomDeleteButton(count = size, modifier = Modifier.align(Alignment.BottomCenter)) {
        val deleteList = mutableListOf<BrowserDownloadItem>()
        selectStateMap.forEach { (item, value) ->
          if (value.value) deleteList.add(item)
        }
        deleteDownloadItems(deleteList)
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

/**
 * 删除界面下面的删除按钮以及确认窗口界面
 */
@Composable
private fun BottomDeleteButton(
  count: Int, modifier: Modifier = Modifier, onDelete: () -> Unit
) {
  val showDialog = remember { mutableStateOf(false) }
  Column(
    modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
      .padding(vertical = 8.dp).height(38.dp).clickableWithNoEffect {
        if (count > 0) showDialog.value = true
      },
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Image(
      imageVector = Icons.Default.Delete,
      contentDescription = "Delete",
      modifier = Modifier.size(20.dp)
    )
    Text(text = button_delete(), fontSize = 12.sp)
  }

  if (showDialog.value) {
    AlertDialog(
      onDismissRequest = { showDialog.value = false },
      buttons = {
        Column(
          modifier = Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
        ) {
          Text(
            text = BrowserDownloadI18nResource.button_delete_confirm().format(count),
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable { onDelete() }
          )
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
          Text(
            text = BrowserDownloadI18nResource.button_cancel(),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable { showDialog.value = false }
          )
        }
      }
    )
  }
}

/**
 * 统一规划顶部工具栏的显示
 */
@Composable
private fun DownloadTopBar(
  title: String,
  onBack: () -> Unit,
  imageVector: ImageVector = Icons.Default.ArrowBackIosNew,
  action: (@Composable RowScope.() -> Unit)? = null
) {
  Row(
    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 8.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Image(
        imageVector = imageVector,
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