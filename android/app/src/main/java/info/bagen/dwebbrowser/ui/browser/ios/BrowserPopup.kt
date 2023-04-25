package info.bagen.dwebbrowser.ui.browser.ios

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.datastore.WebsiteDB
import info.bagen.dwebbrowser.ui.entity.*
import info.bagen.dwebbrowser.ui.theme.DimenBottomBarHeight
import info.bagen.dwebbrowser.util.BitmapUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val screenHeight: Dp
  @Composable get() {
    return LocalConfiguration.current.screenHeightDp.dp
  }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowserPopView(viewModel: BrowserViewModel) {
  if (viewModel.uiState.openBottomSheet.value) {
    ModalBottomSheet(
      onDismissRequest = {
        viewModel.uiState.openBottomSheet.value = false
      },
      sheetState = viewModel.uiState.modalBottomSheetState,
    ) {

      val tabs = listOf(
        TabItem(R.string.browser_nav_option, R.drawable.ic_main_option, PopupViewState.Options),
        TabItem(R.string.browser_nav_book, R.drawable.ic_main_book, PopupViewState.BookList),
        TabItem(
          R.string.browser_nav_history,
          R.drawable.ic_main_history,
          PopupViewState.HistoryList
        ),
      )
      var selectedTabIndex by remember {
        mutableStateOf(0)
      }
      var popupViewState by viewModel.uiState.popupViewState;
      LaunchedEffect(selectedTabIndex) {
        snapshotFlow { selectedTabIndex }.collect {
          popupViewState = tabs[it].entry
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
        PopContentView(viewModel)
      }
    }
  }
}

// 显示具体内容部分，其中又可以分为三个部分类型，操作页，书签列表，历史列表
@Composable
private fun PopContentView(viewModel: BrowserViewModel) {
  Box(modifier = Modifier.fillMaxSize()) {
    when (viewModel.uiState.popupViewState.value) {
      PopupViewState.BookList -> PopContentBookListItem(viewModel)
      PopupViewState.HistoryList -> PopContentHistoryListItem(viewModel)
      else -> PopContentOptionItem(viewModel)
    }
  }
}

@Composable
private fun PopContentOptionItem(viewModel: BrowserViewModel) {
  LazyColumn {
    item { Spacer(modifier = Modifier.fillMaxWidth().height(10.dp)) }
    // 分享和添加书签
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .padding(horizontal = 10.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.background)
          .clickable {
            viewModel.handleIntent(BrowserIntent.SaveBookWebSiteInfo)
          }
      ) {
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
    item { Spacer(modifier = Modifier.fillMaxWidth().height(10.dp)) }
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .padding(horizontal = 10.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.background)
          .clickable {
            viewModel.handleIntent(BrowserIntent.ShareWebSiteInfo)
          }
      ) {
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
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun BoxScope.PopContentBookListItem(viewModel: BrowserViewModel) {
  val scope = rememberCoroutineScope()
  if (viewModel.uiState.bookWebsiteList.isEmpty()) {
    Text(
      text = "未发现书签列表", modifier = Modifier
        .align(TopCenter)
        .padding(top = screenHeight / 5)
    )
    return
  }
  val height = 50.dp
  val popState: MutableState<Triple<Boolean, Int, WebSiteInfo?>> = remember {
    mutableStateOf(Triple(false, 0, null))
  }
  val lazyListState = rememberLazyListState()
  LazyColumn(state = lazyListState) {
    items(viewModel.uiState.bookWebsiteList.size) { index ->
      val webSiteInfo = viewModel.uiState.bookWebsiteList[index]
      if (index > 0) {
        Spacer(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 10.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
        )
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .combinedClickable(
            onClick = {
              scope.launch {
                viewModel.uiState.modalBottomSheetState.hide()
                delay(100)
                viewModel.uiState.openBottomSheet.value = false
              }
              viewModel.handleIntent(BrowserIntent.SearchWebView(webSiteInfo.url))
            },
            onLongClick = { // 弹出一个删除当前，或者删除所有
              popState.value = popState.value.copy(
                first = true, second = index, third = webSiteInfo
              )
            }
          ),
        verticalAlignment = CenterVertically
      ) {
        Icon(
          imageVector = ImageVector.vectorResource(id = R.drawable.ic_main_book),
          contentDescription = "Book",
          modifier = Modifier
            .padding(10.dp)
            .size(30.dp)
        )
        Text(
          text = webSiteInfo.title,
          fontSize = 16.sp,
          maxLines = 1,
          modifier = Modifier.weight(1f)
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
    item {
      Spacer(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(MaterialTheme.colorScheme.outlineVariant)
      )
    }
  }
  PopupListManageView(viewModel, popState, lazyListState, height, ListType.Book)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.PopContentHistoryListItem(viewModel: BrowserViewModel) {
  if (viewModel.uiState.historyWebsiteMap.isEmpty()) {
    Text(
      text = "未发现历史记录", modifier = Modifier
        .align(TopCenter)
        .padding(top = screenHeight / 5)
    )
    return
  }
  Box {
    val scope = rememberCoroutineScope()
    val height = 50.dp
    val popState: MutableState<Triple<Boolean, Int, WebSiteInfo?>> = remember {
      mutableStateOf(Triple(false, 0, null))
    }
    val lazyListState = rememberLazyListState()
    LazyColumn(state = lazyListState) {
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
          Column(modifier = Modifier
            .padding(horizontal = 10.dp)
            .height(height)
            .combinedClickable(
              onClick = {
                scope.launch {
                  viewModel.uiState.modalBottomSheetState.hide()
                  delay(100)
                  viewModel.uiState.openBottomSheet.value = false
                }
                viewModel.handleIntent(BrowserIntent.SearchWebView(webSiteInfo.url))
              },
              onLongClick = {
                popState.value = popState.value.copy(
                  first = true, second = webSiteInfo.index, third = webSiteInfo
                )
              }
            )) {
            Text(
              text = webSiteInfo.title,
              fontSize = 16.sp,
              maxLines = 1,
              modifier = Modifier.height(25.dp)
            )
            Text(
              text = webSiteInfo.url,
              color = MaterialTheme.colorScheme.outlineVariant,
              fontSize = 12.sp,
              maxLines = 1,
              modifier = Modifier.height(20.dp)
            )
            Spacer(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
            )
          }
        }
      }
    }
    PopupListManageView(viewModel, popState, lazyListState, height, ListType.History)
  }
}

@Composable
private fun PopupListManageView(
  viewModel: BrowserViewModel,
  state: MutableState<Triple<Boolean, Int, WebSiteInfo?>>,
  lazyListState: LazyListState,
  height: Dp,
  type: ListType
) {
  if (state.value.first) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val localDensity = LocalDensity.current
    Popup(
      onDismissRequest = { state.value = state.value.copy(first = false) },
      offset = IntOffset(
        (localDensity.run { screenWidth.toPx() } / 2).toInt(),
        (localDensity.run {
          when(type) {
            ListType.Book -> {
              (height * (state.value.second - lazyListState.firstVisibleItemIndex)).toPx()
            }
            ListType.History -> {
              ((height * (state.value.second - lazyListState.firstVisibleItemIndex)) + 30.dp).toPx()
            }
          }
        }).toInt()
      ),
      properties = PopupProperties(),
    ) {
      Column(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .shadow(6.dp)
          .background(MaterialTheme.colorScheme.background)
      ) {
        state.value.third?.let {
          Text(text = "删除当前记录", modifier = Modifier
            .padding(5.dp)
            .clickable {
              state.value.third?.let { item ->
                viewModel.handleIntent(BrowserIntent.DeleteWebSiteList(type, item, false))
              }
              state.value = state.value.copy(first = false)
            })
          Spacer(
            modifier = Modifier
              .height(1.dp)
              .background(MaterialTheme.colorScheme.outlineVariant)
          )
        }
        Text(text = "删除所有记录", modifier = Modifier
          .padding(5.dp)
          .clickable {
            viewModel.handleIntent(BrowserIntent.DeleteWebSiteList(type, null, true))
            state.value = state.value.copy(first = false)
          })
      }
    }
  }
}

/////////////////////////////////////////////////
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
          .background(MaterialTheme.colorScheme.background),
        verticalAlignment = CenterVertically
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
        bitmap = browserBaseView.bitmap
          ?: ImageBitmap.imageResource(id = R.drawable.ic_launcher),
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
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = Modifier.size(12.dp)
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