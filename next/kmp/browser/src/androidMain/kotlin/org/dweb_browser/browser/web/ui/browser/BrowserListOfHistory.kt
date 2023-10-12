package org.dweb_browser.browser.web.ui.browser

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.web.ui.browser.model.BrowserViewModel
import org.dweb_browser.browser.web.ui.browser.model.WebSiteInfo
import org.dweb_browser.browser.web.ui.browser.model.formatToStickyName
import org.dweb_browser.helper.*
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserListOfHistory(
  viewModel: BrowserViewModel,
  @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
  noFoundTip: (@Composable () -> Unit)? = null,
  onSearch: (String) -> Unit
) {
  val list = viewModel.getHistoryLinks()
  val scope = rememberCoroutineScope()
  val currentTime = LocalDate.now().toEpochDay()
  if (list.isEmpty()) {
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
    for (day in currentTime downTo currentTime - 6) {
      val webSiteInfoList = list[day.toString()] ?: break
      stickyHeader {
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

      items(webSiteInfoList.size) { index ->
        val webSiteInfo = webSiteInfoList[index]
        if (index > 0) Divider()
        ListSwipeItem(
          webSiteInfo = webSiteInfo,
          onRemove = {
            scope.launch { viewModel.changeHistoryLink(del = webSiteInfo) }
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