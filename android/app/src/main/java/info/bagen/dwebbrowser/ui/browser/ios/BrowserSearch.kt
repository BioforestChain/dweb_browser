package info.bagen.dwebbrowser.ui.browser.ios

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.ui.entity.BrowserWebView
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
          val text = viewModel.uiState.inputText.value
          Log.e("lin.huang", "xxxxxxxxxxxxxxxxxxx text=$text")
          // 1. 标签页中查找关键字， 2. 搜索引擎， 3. 历史记录， 4.页内查找
          item { // 标签页中查找
            SearchItemForTab(viewModel, text)
          }
          item { // 搜索引擎
            SearchItemEngines(viewModel, text)
          }
          item { // 历史记录
            SearchItemHistory(viewModel, text)
          }
        }
      }
    }
  }
}

@Composable
private fun SearchItemForTab(viewModel: BrowserViewModel, text: String) {
  var firstIndex: Int? = null
  viewModel.uiState.browserViewList.filterIndexed { index, browserBaseView ->
    if (browserBaseView is BrowserWebView && browserBaseView.state.pageTitle?.contains(text) == true) {
      if (firstIndex == null) firstIndex = index
      true
    } else {
      false
    }
  }.firstOrNull()?.also { browserBaseView ->
    if (browserBaseView === viewModel.uiState.currentBrowserBaseView.value) return@also // TODO 如果搜索到的界面就是我当前显示的界面，就不显示该项
    val website = (browserBaseView as BrowserWebView).state.let {
      WebSiteInfo(it.pageTitle ?: "无标题", it.lastLoadedUrl ?: "localhost")
    }
    SearchWebsiteCardView(viewModel, webSiteInfo = website, drawableId = R.drawable.ic_main_multi) {
      // TODO 调转到指定的标签页
      viewModel.handleIntent(BrowserIntent.UpdateMultiViewState(false, firstIndex))
    }
  }
}

@Composable
private fun SearchItemEngines(viewModel: BrowserViewModel, text: String) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "搜索引擎", color = Color.Gray, modifier = Modifier.padding(vertical = 10.dp)
    )
    val list = arrayListOf<WebSiteInfo>().apply {
      add(WebSiteInfo("百度", "https://www.baidu.com/s?wd=$text", null))
      add(WebSiteInfo("谷歌", "https://www.google.com/search?q=$text", null))
      add(WebSiteInfo("必应", "https://cn.bing.com/search?q=$text", null))
      add(WebSiteInfo("搜狗", "https://www.sogou.com/web?query=$text", null))
      add(WebSiteInfo("360", "https://www.so.com/s?q=$text", null))
    }
    Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray))
    list.forEachIndexed { index, webSiteInfo ->
      if (index > 0) {
        Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).padding(start = 40.dp).background(Color.LightGray))
      }
      SearchWebsiteCardView(viewModel, webSiteInfo, index == 0, index == list.size - 1) {
        when (viewModel.uiState.currentBrowserBaseView.value) {
          is BrowserWebView -> viewModel.handleIntent(BrowserIntent.SearchWebView(webSiteInfo.url))
          else -> viewModel.handleIntent(BrowserIntent.AddNewWebView(webSiteInfo.url))
        }
      }
    }
    Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray))
  }
}

@Composable
private fun SearchItemHistory(viewModel: BrowserViewModel, text: String) {
  viewModel.uiState.historyWebsiteMap.firstNotNullOfOrNull {
    it.value.find { websiteInfo -> websiteInfo.title.contains(text) }
  }?.also { websiteInfo ->
    Spacer(modifier = Modifier.height(10.dp))
    SearchWebsiteCardView(viewModel, websiteInfo, drawableId = R.drawable.ic_main_history) {
      // TODO 调转到指定的标签页
      viewModel.handleIntent(BrowserIntent.AddNewWebView(websiteInfo.url))
    }
  }
}

@Composable
fun SearchWebsiteCardView(
  viewModel: BrowserViewModel,
  webSiteInfo: WebSiteInfo,
  isFirst: Boolean = true,
  isLast: Boolean = true,
  @DrawableRes drawableId: Int = R.drawable.ic_web,
  onClick: () -> Unit
) {
  val localFocusManager = LocalFocusManager.current
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .height(50.dp)
      .clip(
        RoundedCornerShape(
          topStart = if (isFirst) 8.dp else 0.dp,
          topEnd = if (isFirst) 8.dp else 0.dp,
          bottomStart = if (isLast) 8.dp else 0.dp,
          bottomEnd = if (isLast) 8.dp else 0.dp
        )
      )
      .background(Color.White)
      .clickable {
        localFocusManager.clearFocus() // 点击后，直接取消聚焦，隐藏键盘
        viewModel.handleIntent(BrowserIntent.UpdateSearchEngineState(false))
        onClick()
      }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = webSiteInfo.icon ?: drawableId,
        contentDescription = null,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = webSiteInfo.title,
          color = Color.Black,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = webSiteInfo.url,
          color = Color.LightGray,
          fontSize = 12.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}