package info.bagen.dwebbrowser.ui.browser

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.ui.entity.BrowserBaseView
import info.bagen.dwebbrowser.ui.entity.BrowserMainView
import info.bagen.dwebbrowser.ui.entity.BrowserWebView
import info.bagen.dwebbrowser.ui.theme.DimenBottomBarHeight
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
  var popupViewState = remember { mutableStateOf(PopupViewState.Options) }
  val tabs = listOf(
    TabItem(R.string.browser_nav_option, R.drawable.ic_main_option, PopupViewState.Options),
    TabItem(R.string.browser_nav_book, R.drawable.ic_main_book, PopupViewState.BookList),
    TabItem(R.string.browser_nav_history, R.drawable.ic_main_history, PopupViewState.HistoryList),
  )

  LaunchedEffect(selectedTabIndex) {
    snapshotFlow { selectedTabIndex }.collect {
      if (it < tabs.size && it >= 0) {
        popupViewState.value = tabs[it].entry
      }
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PopContentView(
  popupViewState: MutableState<PopupViewState>, viewModel: BrowserViewModel
) {
  val bookViewModel = remember { BookViewModel() }
  val historyViewModel = remember { HistoryViewModel() }
  val scope = rememberCoroutineScope()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .navigationBarsPadding()
  ) {
    when (popupViewState.value) {
      PopupViewState.BookList -> BrowserListOfBook(
        bookViewModel,
        onOpenSetting = {
          scope.launch {
            delay(500)
            viewModel.uiState.bottomSheetScaffoldState.bottomSheetState.expand()
          }
        }
      ) {
        scope.launch {
          viewModel.uiState.bottomSheetScaffoldState.bottomSheetState.hide()
          viewModel.handleIntent(BrowserIntent.SearchWebView(it))
        }
      }

      PopupViewState.HistoryList -> BrowserListOfHistory(historyViewModel) {
        scope.launch {
          viewModel.uiState.bottomSheetScaffoldState.bottomSheetState.hide()
          viewModel.handleIntent(BrowserIntent.SearchWebView(it))
        }
      }

      else -> PopContentOptionItem(viewModel)
    }
  }
}

@Composable
private fun PopContentOptionItem(viewModel: BrowserViewModel) {
  // 判断权限
  val launcher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(),
      onResult = { isGranted ->
        //判断权限申请结果，并根据结果显示不同画面，由于 onResult 不是一个 @Composable lambda，所以不能直接显示 Composalbe 需要通过修改 state 等方式间接显示 Composable
        if (isGranted) {
          viewModel.handleIntent(BrowserIntent.ShareWebSiteInfo)
        }
      })
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.outlineVariant)
  ) {
    // 分享和添加书签
    item {
      ListItem(
        modifier = Modifier
          .padding(horizontal = 10.dp, vertical = 5.dp)
          .clip(RoundedCornerShape(8.dp))
          .clickable { viewModel.handleIntent(BrowserIntent.SaveBookWebSiteInfo) },
        colors = ListItemDefaults.colors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
        headlineContent = { Text(text = "添加书签") },
        trailingContent = {
          Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_main_book),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
          )
        }
      )
    }

    item {
      ListItem(
        modifier = Modifier
          .padding(horizontal = 10.dp, vertical = 5.dp)
          .clip(RoundedCornerShape(8.dp))
          .clickable {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
              viewModel.handleIntent(BrowserIntent.ShareWebSiteInfo)
            } else {
              launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)/*请求权限*/
            }
          },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        headlineContent = { Text(text = "分享") },
        trailingContent = {
          Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_main_share),
            contentDescription = "Share",
            modifier = Modifier.size(32.dp)
          )
        }
      )
    }
    item {
      ListItem(
        modifier = Modifier
          .padding(horizontal = 10.dp, vertical = 5.dp)
          .clip(RoundedCornerShape(8.dp)),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        headlineContent = { Text(text = "无痕浏览") },
        trailingContent = {
          Switch(
            checked = viewModel.isNoTrace.value,
            onCheckedChange = { viewModel.saveBrowserMode(it) }
          )
        }
      )
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
          if (browserBaseView.state.lastLoadedUrl?.startsWith("file:///android_asset") == true) {
            Pair("起始页", BitmapUtil.decodeBitmapFromResource(R.drawable.ic_main_star))
          } else {
            Pair(browserBaseView.state.pageTitle, browserBaseView.state.pageIcon)
          }
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

    if (!onlyOne || browserBaseView is BrowserWebView) {
      Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_circle_close),
        contentDescription = "Close",
        modifier = Modifier
          .padding(8.dp)
          .clip(CircleShape)
          .align(Alignment.TopEnd)
          .clickable { viewModel.handleIntent(BrowserIntent.RemoveBaseView(index)) }
          .size(20.dp)
      )
    }
  }
}