package info.bagen.dwebbrowser.ui.browser

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

  LazyColumn(
    modifier = modifier
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 16.dp)
  ) {
    viewModel.historyList.forEach { webSiteInfoList ->
      stickyHeader {
        Text(
          text = webSiteInfoList.key,
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp),
          fontWeight = FontWeight(500),
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.outline
        )
      }

      items(webSiteInfoList.value.size) { index ->
        val webSiteInfo = webSiteInfoList.value[index]
        if (index > 0) Divider()
        ListSwipeItem(
          webSiteInfo = webSiteInfo,
          onRemove = { viewModel.deleteWebSiteInfo(webSiteInfoList, it) }
        ) {
          val shape = when (index) {
            0 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
            webSiteInfoList.value.size - 1 -> RoundedCornerShape(
              bottomStart = 6.dp,
              bottomEnd = 6.dp
            )

            else -> RoundedCornerShape(0.dp)
          }
          RowItemHistory(webSiteInfo, shape) { onSearch(it.url) }
        }
      }
    }
  }
}

@Composable
private fun RowItemHistory(
  webSiteInfo: WebSiteInfo,
  shape: RoundedCornerShape,
  onClick: (WebSiteInfo) -> Unit
) {
  Column(modifier = Modifier
    .fillMaxWidth()
    .background(MaterialTheme.colorScheme.background)
    .height(66.dp)
    .clip(shape)
    .background(MaterialTheme.colorScheme.surface)
    .clickable { onClick(webSiteInfo) }
    .padding(horizontal = 16.dp),
    horizontalAlignment = Alignment.Start
  ) {
    Text(
      text = webSiteInfo.title,
      maxLines = 1,
      fontSize = 16.sp,
      overflow = TextOverflow.Ellipsis,
      fontWeight = FontWeight(400),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .padding(top = 14.dp)
        .fillMaxWidth()
        .height(20.dp)
    )
    Text(
      text = webSiteInfo.url,
      maxLines = 1,
      fontSize = 11.sp,
      overflow = TextOverflow.Ellipsis,
      fontWeight = FontWeight(400),
      color = MaterialTheme.colorScheme.outlineVariant,
      modifier = Modifier
        .padding(top = 4.dp)
        .fillMaxWidth()
        .height(14.dp)
    )
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
      WebSiteDatabase.INSTANCE.websiteDao().loadAllByTypeAscObserve(WebSiteType.History)
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

  fun deleteWebSiteInfo(webSiteInfoList: WebSiteInfoList, webSiteInfo: WebSiteInfo) {
    webSiteInfoList.value.remove(webSiteInfo)
    viewModelScope.launch(ioAsyncExceptionHandler) {
      WebSiteDatabase.INSTANCE.websiteDao().delete(webSiteInfo)
    }
  }
}