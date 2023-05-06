package info.bagen.dwebbrowser.ui.browser.ios

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import coil.compose.AsyncImage
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.ui.entity.BrowserWebView
import info.bagen.dwebbrowser.ui.entity.WebSiteInfo

@SuppressLint("NewApi")
@Composable
fun BrowserSearchPreview(viewModel: BrowserViewModel) {
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp
  val focusManager = LocalFocusManager.current

  if (viewModel.uiState.showSearchEngine.targetState) {
    Box(modifier = Modifier
      .fillMaxSize()
      .padding(bottom = dimenBottomHeight)
      .background(MaterialTheme.colorScheme.outlineVariant)
      .clickable(indication = null,
        onClick = {
          focusManager.clearFocus()
          viewModel.handleIntent(BrowserIntent.UpdateSearchEngineState(false))
        },
        interactionSource = remember { MutableInteractionSource() })) {
      val imeHeight = viewModel.uiState.currentInsets.value.getInsets(WindowInsetsCompat.Type.ime())
      Log.e("lin.huang", "xxxxxxxxxxxxx bottom=${imeHeight.bottom},${imeHeight.top}, $imeHeight")
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(if (imeHeight.bottom > 0) screenHeight / 2 - 30.dp else screenHeight)
          .align(Alignment.BottomCenter)
          .animateContentSize()
      ) {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
        ) {
          item {
            Box(modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)) {
              Text(text = "搜索", modifier = Modifier.align(Alignment.Center), fontSize = 16.sp)
              Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_main_close),
                contentDescription = "Close",
                modifier = Modifier
                  .padding(end = 16.dp)
                  .size(24.dp)
                  .align(Alignment.CenterEnd)
                  .clickable {
                    focusManager.clearFocus()
                    viewModel.handleIntent(BrowserIntent.UpdateSearchEngineState(false))
                  }
              )
            }
          }
          val text = viewModel.uiState.inputText.value
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
      text = "搜索引擎",
      color = MaterialTheme.colorScheme.outline,
      modifier = Modifier.padding(vertical = 10.dp)
    )
    val list = arrayListOf<WebSiteInfo>().apply {
      add(WebSiteInfo("百度", "https://m.baidu.com/s?word=$text", null)) // 百度：https://www.baidu.com/s?wd=你好
      // add(WebSiteInfo("谷歌", "https://www.google.com/search?q=$text", null)) // https://www.google.com/search?q=你好
      // add(WebSiteInfo("必应", "https://cn.bing.com/search?q=$text", null)) // https://cn.bing.com/search?q=hello
      add(WebSiteInfo("搜狗", "https://wap.sogou.com/web/searchList.jsp?keyword=$text", null)) // https://www.sogou.com/web?query=你好
      add(WebSiteInfo("360", "https://m.so.com/s?q=$text", null)) // https://www.so.com/s?q=你好
    }
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(MaterialTheme.colorScheme.outlineVariant)
    )
    list.forEachIndexed { index, webSiteInfo ->
      if (index > 0) {
        Spacer(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 40.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
        )
      }
      SearchWebsiteCardView(viewModel, webSiteInfo, index == 0, index == list.size - 1) {
        viewModel.saveLastKeyword(webSiteInfo.url)
        viewModel.handleIntent(BrowserIntent.SearchWebView(webSiteInfo.url))
      }
    }
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(MaterialTheme.colorScheme.outlineVariant)
    )
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
      viewModel.handleIntent(BrowserIntent.SearchWebView(websiteInfo.url))
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
      .background(MaterialTheme.colorScheme.background)
      .clickable {
        localFocusManager.clearFocus() // 点击后，直接取消聚焦，隐藏键盘
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
          //color = MaterialTheme.colorScheme.surfaceTint,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = webSiteInfo.url,
          color = MaterialTheme.colorScheme.outlineVariant,
          fontSize = 12.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}