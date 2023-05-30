package info.bagen.dwebbrowser.ui.browser

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.bagen.dwebbrowser.database.WebSiteDatabase
import info.bagen.dwebbrowser.database.WebSiteInfo
import info.bagen.dwebbrowser.database.WebSiteType
import info.bagen.dwebbrowser.microService.helper.ioAsyncExceptionHandler
import info.bagen.dwebbrowser.microService.helper.mainAsyncExceptionHandler
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserListOfHistory(
  viewModel: HistoryViewModel = HistoryViewModel(),
  @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
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

  val scope = rememberCoroutineScope()
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
      items(webSiteInfoList.value.size) { index ->
        val webSiteInfo = webSiteInfoList.value[index]
        ListSwipeItem(
          webSiteInfo = webSiteInfo,
          onRemove = {
            webSiteInfoList.value.remove(it)
            scope.launch(ioAsyncExceptionHandler) { WebSiteDatabase.INSTANCE.websiteDao().delete(it) }
          }) {
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
        }
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
          var currentKey: String? = null
          var list: MutableList<WebSiteInfo> = mutableStateListOf()
          historyList.clear()
          it.forEach { webSiteInfo ->
            val stickyName = webSiteInfo.getStickyName()
            if (currentKey != stickyName) {
              currentKey?.let { key ->
                historyList.add(0, WebSiteInfoList(key, list))
              }
              currentKey = stickyName
              list = mutableStateListOf()
            }
            list.add(0, webSiteInfo)
          }
          currentKey?.let { key -> historyList.add(0, WebSiteInfoList(key, list)) }
        }
    }
  }
}