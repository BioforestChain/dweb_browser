package org.dweb_browser.browser.web.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.model.WebSiteInfo
import org.dweb_browser.browser.web.model.formatToStickyName
import org.dweb_browser.browser.web.ui.model.BrowserViewModel
import org.dweb_browser.helper.datetimeNowToEpochDay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserListOfHistory(
  viewModel: BrowserViewModel,
  modifier: Modifier = Modifier,
  noFoundTip: (@Composable () -> Unit)? = null,
  onSearch: (String) -> Unit
) {
  val scope = rememberCoroutineScope()
  if (viewModel.getHistoryLinks().isEmpty()) {
    noFoundTip?.let { it() }
      ?: Box(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = BrowserI18nResource.browser_empty_list(),
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 100.dp)
        )
      }
    return
  }
  val currentDay = datetimeNowToEpochDay()

  LazyColumn(
    modifier = modifier
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 16.dp)
  ) {
    for (day in currentDay downTo currentDay - 6) {
      val webSiteInfoList = viewModel.getHistoryLinks()[day.toString()] ?: continue
      stickyHeader(key = day) {
        Text(
          text = day.formatToStickyName(),
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp),
          fontWeight = FontWeight(500),
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.outline
        )
      }

      itemsIndexed(webSiteInfoList) { index, webSiteInfo ->
        if (index > 0) Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
        ListSwipeItem(
          webSiteInfo = webSiteInfo,
          onRemove = {
            scope.launch {
              webSiteInfoList.remove(webSiteInfo)
              viewModel.removeHistoryLink(webSiteInfo)
            }
          }
        ) {
          val shape = when (index) {
            0 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
            webSiteInfoList.size - 1 -> RoundedCornerShape(
              bottomStart = 6.dp, bottomEnd = 6.dp
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
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = webSiteInfo.title,
      maxLines = 1,
      fontSize = 16.sp,
      overflow = TextOverflow.Ellipsis,
      fontWeight = FontWeight(400),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .height(4.dp)
    )
    Text(
      text = webSiteInfo.url,
      maxLines = 1,
      fontSize = 11.sp,
      overflow = TextOverflow.Ellipsis,
      fontWeight = FontWeight(400),
      color = MaterialTheme.colorScheme.outlineVariant,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

data class WebSiteInfoList(
  val key: String,
  val value: MutableList<WebSiteInfo>,
)