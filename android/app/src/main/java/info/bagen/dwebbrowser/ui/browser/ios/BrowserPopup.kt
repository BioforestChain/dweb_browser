package info.bagen.dwebbrowser.ui.browser.ios

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.datastore.WebsiteDB
import info.bagen.dwebbrowser.ui.entity.*
import info.bagen.dwebbrowser.ui.theme.DimenBottomBarHeight
import info.bagen.dwebbrowser.ui.view.ListItemDeleteView
import info.bagen.dwebbrowser.util.BitmapUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val screenHeight: Dp
  @Composable get() {
    return LocalConfiguration.current.screenHeightDp.dp
  }

enum class PopupViewState(
  private val height: Dp = 0.dp,
  private val percentage: Float? = null,
  val title: String,
) {
  Options(height = 120.dp, title = "选项"),
  BookList(percentage = 0.9f, title = "书签列表"),
  HistoryList(percentage = 0.9f, title = "历史记录"),
  Share(percentage = 0.5f, title = "分享");

  fun getLocalHeight(screenHeight: Dp? = null): Dp {
    return screenHeight?.let { screenHeight ->
      percentage?.let { percentage ->
        screenHeight * percentage
      }
    } ?: height
  }
}

class TabItem(
  @StringRes val title_res: Int,
  @DrawableRes val icon_res: Int,
  val entry: PopupViewState
) {
  val title @Composable get() = stringResource(id = title_res)
  val icon @Composable get() = ImageVector.vectorResource(id = icon_res)
}

@Composable
internal fun BrowserPopView(viewModel: BrowserViewModel) {
  var selectedTabIndex by remember { mutableStateOf(0) }
  val popupViewState = remember { mutableStateOf(PopupViewState.Options) }
  val tabs = when (viewModel.uiState.currentBrowserBaseView.value) {
    is BrowserWebView -> {
      listOf(
        TabItem(R.string.browser_nav_option, R.drawable.ic_main_option, PopupViewState.Options),
        TabItem(R.string.browser_nav_book, R.drawable.ic_main_book, PopupViewState.BookList),
        TabItem(
          R.string.browser_nav_history, R.drawable.ic_main_history, PopupViewState.HistoryList
        ),
      )
    }
    else -> {
      listOf(
        TabItem(R.string.browser_nav_book, R.drawable.ic_main_book, PopupViewState.BookList),
        TabItem(
          R.string.browser_nav_history, R.drawable.ic_main_history, PopupViewState.HistoryList
        ),
      )
    }
  }

  LaunchedEffect(selectedTabIndex) {
    snapshotFlow { selectedTabIndex }.collect {
      popupViewState.value = tabs[it].entry
    }
  }

  Column {
    TabRow(selectedTabIndex = selectedTabIndex) {
      tabs.forEachIndexed { index, tabItem ->
        Tab(
          selected = selectedTabIndex == index,
          onClick = { selectedTabIndex = index },
          icon = {
            Icon(
              imageVector = tabItem.icon,
              contentDescription = tabItem.title,
              modifier = Modifier.size(24.dp)
            )
          },
        )
      }
    }
    PopContentView(popupViewState, viewModel)
  }
}

// 显示具体内容部分，其中又可以分为三个部分类型，操作页，书签列表，历史列表
@Composable
private fun PopContentView(
  popupViewState: MutableState<PopupViewState>, viewModel: BrowserViewModel
) {
  Box(modifier = Modifier.fillMaxSize()) {
    when (popupViewState.value) {
      PopupViewState.BookList -> PopContentBookListItem(viewModel)
      PopupViewState.HistoryList -> PopContentHistoryListItem(viewModel)
      else -> PopContentOptionItem(viewModel)
    }
  }
}

@Composable
private fun PopContentOptionItem(viewModel: BrowserViewModel) {
  val launcher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(),
      onResult = { isGranted ->
        //判断权限申请结果，并根据结果侠士不同画面，由于 onResult 不是一个 @Composable lambda，所以不能直接显示 Composalbe 需要通过修改 state 等方式间接显示 Composable
        if (isGranted) {
          viewModel.handleIntent(BrowserIntent.ShareWebSiteInfo)
        }
      })
  LazyColumn {
    item {
      Spacer(
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp)
      )
    }
    // 分享和添加书签
    item {
      Box(modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .padding(horizontal = 10.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.background)
        .clickable {
          viewModel.handleIntent(BrowserIntent.SaveBookWebSiteInfo)
        }) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = CenterVertically) {
          Text(
            text = "添加书签", modifier = Modifier
              .weight(1f)
              .padding(horizontal = 10.dp)
          )
          Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_main_book),
            contentDescription = null,
            modifier = Modifier
              .padding(15.dp)
              .size(30.dp)
          )
        }

      }
    }
    item {
      Spacer(
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp)
      )
      Box(modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .padding(horizontal = 10.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.background)
        .clickable {
          launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) // 请求权限
          //viewModel.handleIntent(BrowserIntent.ShareWebSiteInfo)
        }) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = CenterVertically) {
          Text(
            text = "分享", modifier = Modifier
              .weight(1f)
              .padding(horizontal = 10.dp)
          )
          Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_main_share),
            contentDescription = null,
            modifier = Modifier
              .padding(15.dp)
              .size(30.dp)
          )
        }
      }
    }
    item {
      Spacer(
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp)
      )
      Box(modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .padding(horizontal = 10.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.background)
        .clickable {
          launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) // 请求权限
          //viewModel.handleIntent(BrowserIntent.ShareWebSiteInfo)
        }) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = CenterVertically) {
          Text(
            text = "无痕浏览", modifier = Modifier
              .weight(1f)
              .padding(horizontal = 10.dp)
          )
          Switch(
            checked = viewModel.isNoTrace.value,
            onCheckedChange = { viewModel.saveBrowserMode(it) },
            modifier = Modifier
              .height(30.dp)
              .padding(15.dp)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.PopContentBookListItem(viewModel: BrowserViewModel) {
  if (viewModel.uiState.bookWebsiteList.isEmpty()) {
    Text(
      text = "未发现书签列表", modifier = Modifier
        .align(TopCenter)
        .padding(top = screenHeight / 5)
    )
    return
  }
  val scope = rememberCoroutineScope()
  LazyColumn {
    items(viewModel.uiState.bookWebsiteList.size) { index ->
      val webSiteInfo = viewModel.uiState.bookWebsiteList[index]
      ListItemDeleteView(
        onClick = {
          scope.launch {
            viewModel.uiState.bottomSheetScaffoldState.bottomSheetState.hide()
          }
          viewModel.handleIntent(BrowserIntent.SearchWebView(webSiteInfo.url))
        },
        onDelete = {
          viewModel.handleIntent(BrowserIntent.DeleteWebSiteList(ListType.Book, webSiteInfo, false))
        },
        enableExpand = true,
        expandContent = {
          ExpandTextFiled("书签名称", webSiteInfo.title) {
            webSiteInfo.title = it
            WebsiteDB.saveBookWebsiteInfo(webSiteInfo)
          }
          ExpandTextFiled("网址详情", webSiteInfo.url) {
            webSiteInfo.url = it
            WebsiteDB.saveBookWebsiteInfo(webSiteInfo)
          }
        }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp), verticalAlignment = CenterVertically
        ) {
          Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_main_book),
            contentDescription = "Book",
            modifier = Modifier
              .padding(10.dp)
              .size(30.dp)
          )
          Text(
            text = webSiteInfo.title, fontSize = 16.sp, maxLines = 1, modifier = Modifier.weight(1f)
          )
          Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_more),
            contentDescription = "More",
            modifier = Modifier
              .padding(10.dp)
              .size(30.dp)
              .graphicsLayer(rotationX = 90f)
          )
        }
      }
    }
  }
}

@Composable
fun ExpandTextFiled(
  label: String,
  title: String,
  modifier: Modifier = Modifier.fillMaxWidth(),
  onValueChanged: (String) -> Unit
) {
  var text by remember { mutableStateOf(title) }
  OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    modifier = modifier
      .background(MaterialTheme.colorScheme.surface)
      .padding(10.dp),
    label = { Text(text = label) },
    singleLine = true,
    maxLines = 1,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(onDone = {
      if (text.isEmpty()) return@KeyboardActions
      onValueChanged(text)
    }),
    trailingIcon = {
      Icon(Icons.Default.Done, contentDescription = "Done")
    }
  )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.PopContentHistoryListItem(viewModel: BrowserViewModel) {
  if (viewModel.isNoTrace.value || viewModel.uiState.historyWebsiteMap.isEmpty()) {
    Text(
      text = "未发现历史记录", modifier = Modifier
        .align(TopCenter)
        .padding(top = screenHeight / 5)
    )
    return
  }
  val scope = rememberCoroutineScope()
  Box {
    LazyColumn {
      viewModel.uiState.historyWebsiteMap.toSortedMap { o1, o2 ->
        if (o1 < o2) 1 else -1
      }.forEach { (key, value) ->
        stickyHeader {
          Text(
            text = WebsiteDB.compareWithLocalTime(key),
            modifier = Modifier
              .fillMaxWidth()
              .height(30.dp)
              .background(MaterialTheme.colorScheme.outlineVariant)
              .padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
        items(value.size) { index ->
          val webSiteInfo = value[index]
          ListItemDeleteView(
            onClick = {
              scope.launch {
                viewModel.uiState.bottomSheetScaffoldState.bottomSheetState.hide()
              }
              viewModel.handleIntent(BrowserIntent.SearchWebView(webSiteInfo.url))
            },
            onDelete = {
              viewModel.handleIntent(
                BrowserIntent.DeleteWebSiteList(
                  ListType.History,
                  webSiteInfo,
                  false
                )
              )
            }
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart)
            ) {
              Text(
                text = webSiteInfo.title,
                fontSize = 16.sp,
                maxLines = 1,
                modifier = Modifier.height(25.dp),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = webSiteInfo.url,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.height(20.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * 显示多视图窗口
 */
@Composable
internal fun BrowserMultiPopupView(viewModel: BrowserViewModel) {
  val browserViewList = viewModel.uiState.browserViewList
  AnimatedVisibility(visibleState = viewModel.uiState.multiViewShow) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.outline)
        .clickable(indication = null,
          onClick = { },
          interactionSource = remember { MutableInteractionSource() })
    ) {
      if (browserViewList.size == 1) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          Box(
            modifier = Modifier
              .align(TopCenter)
              .padding(top = 20.dp)
          ) {
            MultiItemView(viewModel, browserViewList[0], true)
          }
        }
      } else {
        val lazyGridState = rememberLazyGridState()
        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          modifier = Modifier.weight(1f),
          state = lazyGridState,
          contentPadding = PaddingValues(vertical = 20.dp, horizontal = 20.dp),
          horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
          items(browserViewList.size) {
            MultiItemView(viewModel, browserViewList[it], index = it)
          }
        }
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(DimenBottomBarHeight)
          .background(MaterialTheme.colorScheme.background), verticalAlignment = CenterVertically
      ) {
        Icon(
          imageVector = ImageVector.vectorResource(id = R.drawable.ic_main_add),
          contentDescription = "Add",
          modifier = Modifier
            .padding(start = 8.dp, end = 8.dp)
            .size(32.dp)
            .align(CenterVertically)
            .clickable { viewModel.handleIntent(BrowserIntent.AddNewMainView) },
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "${browserViewList.size}个标签页",
          modifier = Modifier.weight(1f),
          textAlign = TextAlign.Center
        )
        Text(
          text = "完成",
          modifier = Modifier
            .padding(start = 8.dp, end = 8.dp)
            .clickable { viewModel.handleIntent(BrowserIntent.UpdateMultiViewState(false)) },
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun MultiItemView(
  viewModel: BrowserViewModel,
  browserBaseView: BrowserBaseView,
  onlyOne: Boolean = false,
  index: Int = 0
) {
  val screenWidth = LocalConfiguration.current.screenWidthDp.dp
  val sizeTriple = if (onlyOne) {
    val with = screenWidth - 120.dp
    Triple(with, with * 9 / 6 - 60.dp, with * 9 / 6)
  } else {
    val with = (screenWidth - 60.dp) / 2
    Triple(with, with * 9 / 6 - 40.dp, with * 9 / 6)
  }
  Box(modifier = Modifier.size(width = sizeTriple.first, height = sizeTriple.third)) {
    Column(horizontalAlignment = CenterHorizontally, modifier = Modifier.clickable {
      viewModel.handleIntent(BrowserIntent.UpdateMultiViewState(false, index))
    }) {
      val color =
        if (browserBaseView == viewModel.uiState.currentBrowserBaseView.value && !onlyOne) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.outline
        }
      Image(
        bitmap = browserBaseView.bitmap ?: ImageBitmap.imageResource(id = R.drawable.ic_launcher),
        contentDescription = null,
        modifier = Modifier
          .size(width = sizeTriple.first, height = sizeTriple.second)
          .clip(RoundedCornerShape(16.dp))
          .background(color)
          .padding(2.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.outline)
          .padding(2.dp)
          .clip(RoundedCornerShape(16.dp))
          .align(CenterHorizontally),
        contentScale = ContentScale.FillWidth, //ContentScale.FillBounds,
        alignment = if (browserBaseView is BrowserMainView) Center else TopStart
      )
      val contentPair = when (browserBaseView) {
        is BrowserMainView -> {
          Pair("起始页", BitmapUtil.decodeBitmapFromResource(R.drawable.ic_main_star))
        }
        is BrowserWebView -> {
          Pair(browserBaseView.state.pageTitle, browserBaseView.state.pageIcon)
        }
        else -> {
          Pair(null, null)
        }
      }
      Row(
        modifier = Modifier
          .width(sizeTriple.first)
          .align(CenterHorizontally)
          .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = CenterVertically
      ) {
        contentPair.second?.asImageBitmap()?.let { imageBitmap ->
          Icon(
            bitmap = imageBitmap, contentDescription = null, modifier = Modifier.size(12.dp)
          )
        }
        Text(
          text = contentPair.first ?: "无标题",
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          fontSize = 12.sp
        )
      }
    }

    if (!onlyOne) {
      Box(modifier = Modifier
        .padding(8.dp)
        .clip(CircleShape)
        .align(Alignment.TopEnd)
        .background(MaterialTheme.colorScheme.outlineVariant)
        .clickable {
          viewModel.handleIntent(BrowserIntent.RemoveBaseView(index))
        }) {
        Icon(
          imageVector = ImageVector.vectorResource(id = R.drawable.ic_main_close),
          contentDescription = null,
          modifier = Modifier.size(18.dp),
          tint = MaterialTheme.colorScheme.outline
        )
      }
    }
  }
}