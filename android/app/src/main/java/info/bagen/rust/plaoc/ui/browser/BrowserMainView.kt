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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import info.bagen.rust.plaoc.R
import kotlinx.coroutines.delay

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
private fun IconView(model: Any?, text: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.size(64.dp, 100.dp)) {
    AsyncImage(
      model = model,
      contentDescription = text,
      modifier = Modifier
        .size(64.dp)
        .clip(RoundedCornerShape(16.dp))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = text, modifier = Modifier.align(Alignment.CenterHorizontally), maxLines = 2)
  }
}

@Composable
private fun HotWebSiteView(viewModel: BrowserViewModel) {
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
      horizontalArrangement = Arrangement.spacedBy(20.dp),
      verticalArrangement = Arrangement.spacedBy(0.dp),
      modifier = Modifier.height(200.dp)
    ) {
      items(initHotWebsite) {
        IconView(model = it.iconUrl, text = it.name, modifier = Modifier.clickable {
          viewModel.handleIntent(BrowserIntent.AddNewWebView(it.webUrl))
        })
      }
    }
  }
}

@Composable
private fun HotSearchView(viewModel: BrowserViewModel) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(20.dp)
  ) {
    TitleText(id = R.string.browser_main_hot_search)
    Spacer(modifier = Modifier.height(10.dp))


    val loadedState = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
      delay(10000)
      loadedState.value = true
    }
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
      initialValue = 0.6f, targetValue = 0.1f, animationSpec = infiniteRepeatable(
        animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Reverse
      )
    )
    Column(modifier = Modifier.alpha(if (loadedState.value) 1f else alpha)) {
      Box(
        modifier = Modifier
          .size(320.dp, 22.dp)
          .background(Color.LightGray)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Box(
        modifier = Modifier
          .size(100.dp, 22.dp)
          .background(Color.LightGray)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Box(
        modifier = Modifier
          .size(230.dp, 22.dp)
          .background(Color.LightGray)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Box(
        modifier = Modifier
          .size(120.dp, 22.dp)
          .background(Color.LightGray)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Box(
        modifier = Modifier
          .size(200.dp, 22.dp)
          .background(Color.LightGray)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Box(
        modifier = Modifier
          .size(260.dp, 22.dp)
          .background(Color.LightGray)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Box(
        modifier = Modifier
          .size(320.dp, 22.dp)
          .background(Color.LightGray)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Box(
        modifier = Modifier
          .size(100.dp, 22.dp)
          .background(Color.LightGray)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Box(
        modifier = Modifier
          .size(230.dp, 22.dp)
          .background(Color.LightGray)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Box(
        modifier = Modifier
          .size(120.dp, 22.dp)
          .background(Color.LightGray)
      )
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}


data class HotWebsite(
  val name: String,
  val iconUrl: String,
  val webUrl: String,
)

private val initHotWebsite = mutableListOf<HotWebsite>().also {
  it.add(HotWebsite("斗鱼", "http://linge.plaoc.com/douyu.png", "https://www.douyu.com/"))
  it.add(HotWebsite("网易", "http://linge.plaoc.com/163.png", "https://www.163.com/"))
  it.add(HotWebsite("微博", "http://linge.plaoc.com/weibo.png", "https://weibo.com/"))
  it.add(HotWebsite("豆瓣", "http://linge.plaoc.com/douban.png", "https://movie.douban.com/"))
  it.add(HotWebsite("知乎", "http://linge.plaoc.com/zhihu.png", "https://www.zhihu.com/"))
  it.add(HotWebsite("哔哩哔哩", "http://linge.plaoc.com/bilibili.png", "https://www.bilibili.com/"))
  it.add(HotWebsite("腾讯新闻", "http://linge.plaoc.com/tencent.png", "https://www.qq.com/"))
  it.add(HotWebsite("京东", "http://linge.plaoc.com/jingdong.png", "https://www.jd.com/"))
}