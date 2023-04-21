package info.bagen.dwebbrowser.ui.browser.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.ui.entity.WebSiteInfo

@Composable
fun BrowserSearchPreview(viewModel: BrowserViewModel) {
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp
  if (viewModel.uiState.showSearchEngine.targetState) {
    Box(modifier = Modifier
      .fillMaxSize()
      .padding(bottom = dimenBottomHeight)
      .background(Color.LightGray)
      .clickable(enabled = false) {}) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(screenHeight / 2 - 20.dp)
          .align(Alignment.BottomCenter)
      ) {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
        ) {
          item {
            if (true) {
              SearchWebsiteCardView(WebSiteInfo("找到的书签", "地址信息", null), true, true)
            }
          }
          item {
            Text(
              text = "搜索引擎", color = Color.Gray, modifier = Modifier.padding(vertical = 10.dp)
            )
          }
          val list = getSearchEngines("测试")
          items(list.size) { index ->
            SearchWebsiteCardView(list[index], index == 0, index == list.size - 1)
          }
        }
      }
    }
  }
}

private fun getSearchEngines(name: String) = run {
  arrayListOf<WebSiteInfo>().apply {
    add(WebSiteInfo("百度搜索", "https://www.baidu.com/s?wd=$name", null))
    add(WebSiteInfo("谷歌搜索", "https://www.google.com/search?q=$name", null))
    add(WebSiteInfo("必应搜索", "https://cn.bing.com/search?q=$name", null))
    add(WebSiteInfo("搜狗搜索", "https://www.sogou.com/web?query=$name", null))
    add(WebSiteInfo("360搜索", "https://www.so.com/s?q=$name", null))
  }
}

@Composable
fun SearchWebsiteCardView(
  webSiteInfo: WebSiteInfo, isFirst: Boolean = false, isLast: Boolean = false
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(
        RoundedCornerShape(
          topStart = if (isFirst) 8.dp else 0.dp,
          topEnd = if (isFirst) 8.dp else 0.dp,
          bottomStart = if (isLast) 8.dp else 0.dp,
          bottomEnd = if (isLast) 8.dp else 0.dp
        )
      )
      .background(Color.White)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(model = webSiteInfo.icon ?: R.drawable.ic_launcher, contentDescription = null)
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = webSiteInfo.title, color = Color.Black, fontWeight = FontWeight.Bold)
        Text(text = webSiteInfo.url, color = Color.LightGray, fontSize = 12.sp)
      }
    }
  }
}