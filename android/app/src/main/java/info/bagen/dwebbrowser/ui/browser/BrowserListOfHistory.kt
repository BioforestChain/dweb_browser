package info.bagen.dwebbrowser.ui.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.bagen.dwebbrowser.database.WebSiteDatabase
import info.bagen.dwebbrowser.database.WebSiteInfo
import info.bagen.dwebbrowser.database.WebSiteType
import info.bagen.dwebbrowser.microService.helper.mainAsyncExceptionHandler
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserListOfHistory(
  viewModel: HistoryViewModel = HistoryViewModel(),
  modifier: Modifier = Modifier,
  noFoundTip: (@Composable () -> Unit)? = null,
  onSearch: (String) -> Unit
) {
  if (viewModel.historyList.isEmpty()) {
    noFoundTip?.let { it() }
      ?: Box(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "暂无数据",
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 100.dp)
        )
      }
    return
  }

  LazyColumn(modifier = modifier) {
    viewModel.historyList.forEach { webSiteInfoList ->
      stickyHeader {
        Text(
          text = webSiteInfoList.key,
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.outlineVariant)
            .padding(10.dp)
        )
      }
      items(webSiteInfoList.value) { webSiteInfo ->
        ListItem(
          headlineContent = {
            Text(text = webSiteInfo.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
          },
          supportingContent = {
            Text(text = webSiteInfo.url, maxLines = 1, overflow = TextOverflow.Ellipsis)
          },
          modifier = Modifier.clickable {
            onSearch(webSiteInfo.url)
          }
        )
        Divider()
      }
    }
  }
}

data class WebSiteInfoList(
  val key: String,
  val value: MutableList<WebSiteInfo>,
)

class HistoryViewModel : ViewModel() {
  val historyList: MutableList<WebSiteInfoList> = mutableStateListOf()

  init {
    viewModelScope.launch(mainAsyncExceptionHandler) {
      WebSiteDatabase.INSTANCE.websiteDao().loadAllByTypeObserve(WebSiteType.History)
        .observeForever {
          if (historyList.isEmpty()) { // 如果是空的，属于第一次加载，整体填充
            var currentKey: String? = null
            var list: MutableList<WebSiteInfo> = mutableListOf()
            it.forEach { webSiteInfo ->
              val stickyName = webSiteInfo.getStickyName()
              if (currentKey != stickyName) {
                currentKey?.let { key ->
                  historyList.add(0, WebSiteInfoList(key, list))
                }
                currentKey = stickyName
                list = mutableListOf()
              }
              list.add(0, webSiteInfo)
            }
            currentKey?.let { key -> historyList.add(0, WebSiteInfoList(key, list)) }
          } else {
            val today = LocalDate.now().toEpochDay()
            val list: MutableList<WebSiteInfo> = mutableListOf()
            it.filterIndexed { _, webSiteInfo -> webSiteInfo.timeMillis == today }
              .forEach { webSiteInfo -> list.add(0, webSiteInfo) }
            historyList.removeIf { webSiteInfoList -> webSiteInfoList.key == "今天" }
            historyList.add(0, WebSiteInfoList("今天", list))
          }
        }
    }
  }
}