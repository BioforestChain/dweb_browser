package info.bagen.rust.plaoc.ui.browser

import androidx.annotation.StringRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import info.bagen.rust.plaoc.R

@Composable
fun BrowserMainView(viewModel: BrowserViewModel) {
  LazyColumn {
    item { HotWebSiteView(viewModel) }
    item { HotSearchView(viewModel) }
  }

  /*Home(
    mainViewModel = MainViewModel(),
    appViewModel = AppViewModel(),
    onSearchAction = { action, data ->
      when (action) {
        SearchAction.Search -> {}
        SearchAction.OpenCamera -> {}
      }
    }, onOpenDWebview = { appId, dAppInfo ->
      // TODO 这里是点击桌面app触发的事件
    }
  )*/
}

@Composable
private fun TitleText(@StringRes id: Int) {
  Text(text = stringResource(id = id), fontSize = 22.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun IconView(model: Any?, text: String, onClick: () -> Unit) {
  Column(modifier = Modifier.size(64.dp, 100.dp)) {
    AsyncImage(
      model = model,
      contentDescription = text,
      modifier = Modifier
        .size(64.dp)
        .clip(RoundedCornerShape(16.dp))
        .clickable { onClick() }
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = text, modifier = Modifier.align(Alignment.CenterHorizontally), maxLines = 2)
  }
}

@Composable
private fun HotWebSiteView(viewModel: BrowserViewModel) {
  val screenWidth = LocalConfiguration.current.screenWidthDp.dp
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(20.dp)
  ) {
    TitleText(id = R.string.browser_main_hot_web)
    Spacer(modifier = Modifier.height(10.dp))
    LazyHorizontalGrid(
      rows = GridCells.Fixed(2),
      contentPadding = PaddingValues(horizontal = 0.dp),
      horizontalArrangement = Arrangement.spacedBy((screenWidth - 64.dp * 4 - 40.dp) / 3),
      verticalArrangement = Arrangement.spacedBy(0.dp),
      modifier = Modifier.height(200.dp)
    ) {
      items(initHotWebsite) {
        IconView(model = it.iconUrl, text = it.name) {
          viewModel.handleIntent(BrowserIntent.AddNewWebView(it.webUrl))
        }
      }
    }
  }
}

@Composable
private fun HotSearchView(viewModel: BrowserViewModel) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
  ) {
    TitleText(id = R.string.browser_main_hot_search)
    Spacer(modifier = Modifier.height(10.dp))

    viewModel.uiState.hotLinkList.forEachOrEmpty(
      empty = { ListLoadingView() }
    ) {
      Text(text = it.showHotText(), maxLines = 1, modifier = Modifier.clickable {
        viewModel.handleIntent(BrowserIntent.AddNewWebView(it.webUrl))
      })
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

public inline fun <T> Iterable<T>.forEachOrEmpty(empty: () -> Unit, action: (T) -> Unit): Unit {
  if (this.count() > 0) {
    for (element in this) action(element)
  } else {
    empty()
  }
}

@Composable
private fun ListLoadingView() {
  val infiniteTransition = rememberInfiniteTransition()
  val alpha by infiniteTransition.animateFloat(
    initialValue = 0.8f, targetValue = 0.3f, animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse
    )
  )
  Column(modifier = Modifier.alpha(alpha)) {
    Box(
      modifier = Modifier
        .size(320.dp, 16.dp)
        .background(Color.LightGray)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Box(
      modifier = Modifier
        .size(100.dp, 16.dp)
        .background(Color.LightGray)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Box(
      modifier = Modifier
        .size(230.dp, 16.dp)
        .background(Color.LightGray)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Box(
      modifier = Modifier
        .size(120.dp, 16.dp)
        .background(Color.LightGray)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Box(
      modifier = Modifier
        .size(200.dp, 16.dp)
        .background(Color.LightGray)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Box(
      modifier = Modifier
        .size(260.dp, 16.dp)
        .background(Color.LightGray)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Box(
      modifier = Modifier
        .size(320.dp, 16.dp)
        .background(Color.LightGray)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Box(
      modifier = Modifier
        .size(100.dp, 16.dp)
        .background(Color.LightGray)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Box(
      modifier = Modifier
        .size(230.dp, 16.dp)
        .background(Color.LightGray)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Box(
      modifier = Modifier
        .size(120.dp, 16.dp)
        .background(Color.LightGray)
    )
    Spacer(modifier = Modifier.height(16.dp))
  }
}

private val initHotWebsite = mutableListOf<WebSiteInfo>().also {
  it.add(WebSiteInfo(name = "斗鱼", iconUrl = "http://linge.plaoc.com/douyu.png", webUrl = "https://m.douyu.com/"))
  it.add(WebSiteInfo(name = "网易", iconUrl = "http://linge.plaoc.com/163.png", webUrl = "https://3g.163.com/"))
  it.add(WebSiteInfo(name = "微博", iconUrl = "http://linge.plaoc.com/weibo.png", webUrl = "https://m.weibo.cn/"))
  it.add(WebSiteInfo(name = "豆瓣", iconUrl = "http://linge.plaoc.com/douban.png", webUrl = "https://m.douban.com/movie/"))
  it.add(WebSiteInfo(name = "知乎", iconUrl = "http://linge.plaoc.com/zhihu.png", webUrl = "https://www.zhihu.com/"))
  it.add(WebSiteInfo(name = "哔哩哔哩", iconUrl = "http://linge.plaoc.com/bilibili.png", webUrl = "https://m.bilibili.com/"))
  it.add(WebSiteInfo(name = "腾讯新闻", iconUrl = "http://linge.plaoc.com/tencent.png", webUrl = "https://xw.qq.com/?f=qqcom"))
  it.add(WebSiteInfo(name = "京东", iconUrl = "http://linge.plaoc.com/jingdong.png", webUrl = "https://m.jd.com/"))
}