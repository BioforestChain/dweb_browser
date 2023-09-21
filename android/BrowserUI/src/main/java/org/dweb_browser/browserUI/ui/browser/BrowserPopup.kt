package org.dweb_browser.browserUI.ui.browser

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.R
import org.dweb_browser.browserUI.database.WebSiteDatabase
import org.dweb_browser.browserUI.database.WebSiteInfo
import org.dweb_browser.browserUI.ui.browser.bottomsheet.BrowserModalBottomSheet
import org.dweb_browser.browserUI.ui.browser.bottomsheet.LocalModalBottomSheet
import org.dweb_browser.browserUI.ui.browser.search.CustomTextField
import org.dweb_browser.browserUI.ui.entity.BrowserBaseView
import org.dweb_browser.browserUI.ui.entity.BrowserMainView
import org.dweb_browser.browserUI.ui.entity.BrowserWebView
import org.dweb_browser.browserUI.ui.theme.DimenBottomBarHeight
import org.dweb_browser.browserUI.ui.view.LocalCommonUrl
import org.dweb_browser.browserUI.ui.view.findActivity
import org.dweb_browser.browserUI.util.BitmapUtil
import org.dweb_browser.browserUI.util.PrivacyUrl
import org.dweb_browser.helper.compose.rememberPlatformViewController
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.PlatformViewController
import org.dweb_browser.helper.platform.getCornerRadiusTop

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
  @StringRes val titleRid: Int,
  @DrawableRes val iconRid: Int,
  val entry: PopupViewState
) {
  val title @Composable get() = stringResource(id = titleRid)
  val icon @Composable get() = ImageVector.vectorResource(id = iconRid)
}

@Composable
internal fun BrowserBottomSheet(viewModel: BrowserViewModel) {
  val bottomSheetModel = LocalModalBottomSheet.current
  if (bottomSheetModel.show.value) {
    val density = LocalDensity.current.density
    val topLeftRadius = getCornerRadiusTop(rememberPlatformViewController(), density, 16f)
    BrowserModalBottomSheet(
      onDismissRequest = { bottomSheetModel.show.value = false },
      shape = RoundedCornerShape(
        topStart = topLeftRadius * density,
        topEnd = topLeftRadius * density
      )
    ) {
      BrowserPopView(viewModel)
    }
  }
}

/**
 * 弹出主界面，包括了三个tab和一个书签管理界面 TODO 目前缺少切换到书签管理界面后的展开问题
 */
@Composable
internal fun BrowserPopView(viewModel: BrowserViewModel) {
  val selectedTabIndex = remember { mutableIntStateOf(0) }
  val pageIndex = remember { mutableIntStateOf(0) }
  var webSiteInfo: WebSiteInfo? = null
  /*val scope = rememberCoroutineScope()

  LaunchedEffect(pageIndex) {
    snapshotFlow { pageIndex.value }.collect {
      if (it == 1) {
        delay(200)
        scope.launch { viewModel.uiState.bottomSheetScaffoldState.bottomSheetState.expand() }
      }
    }
  }*/

  AnimatedContent(targetState = pageIndex, label = "",
    transitionSpec = {
      if (targetState.intValue > initialState.intValue) {
        // 数字变大时，进入的界面从右向左变深划入，退出的界面从右向左变浅划出
        (slideInHorizontally { fullWidth -> fullWidth } + fadeIn()).togetherWith(
          slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut())
      } else {
        // 数字变小时，进入的数字从左向右变深划入，退出的数字从左向右变浅划出
        (slideInHorizontally { fullWidth -> -fullWidth } + fadeIn()).togetherWith(
          slideOutHorizontally { fullWidth -> fullWidth } + fadeOut())
      }
    }
  ) { targetPage ->
    when (targetPage.intValue) {
      0 -> {
        PopTabRowContent(
          viewModel = viewModel,
          selectedTabIndex = selectedTabIndex,
          openBookManager = {
            webSiteInfo = it
            pageIndex.intValue = 1
          }
        )
      }

      1 -> {
        PopBookManagerView(webSiteInfo = webSiteInfo) { pageIndex.intValue = 0 }
      }

      else -> {}
    }
  }
}

/**
 * 书签管理界面
 */
@Composable
private fun PopBookManagerView(webSiteInfo: WebSiteInfo?, onBack: () -> Unit) {
  val scope = rememberCoroutineScope()
  val inputTitle = remember { mutableStateOf(webSiteInfo?.title ?: "") }
  val inputUrl = remember { mutableStateOf(webSiteInfo?.url ?: "") }
  Box(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp), verticalAlignment = CenterVertically
    ) {
      Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_main_back),
        contentDescription = "Back",
        modifier = Modifier
          .clickable { onBack() }
          .padding(horizontal = 16.dp)
          .size(22.dp),
        tint = MaterialTheme.colorScheme.onBackground
      )
      Text(
        text = "编辑书签",
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center,
        fontSize = 18.sp
      )
      Text(
        text = "存储",
        modifier = Modifier
          .clickable {
            webSiteInfo?.apply {
              title = inputTitle.value
              url = inputUrl.value
              scope.launch(ioAsyncExceptionHandler) {
                WebSiteDatabase.INSTANCE
                  .websiteDao()
                  .update(this@apply)
              }
              onBack()
            }
          }
          .padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 18.sp
      )
    }
    val item = webSiteInfo ?: return
    val focusRequester = FocusRequester()
    LaunchedEffect(focusRequester) {
      delay(500)
      focusRequester.requestFocus()
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp, top = 56.dp)
    ) {
      RowItemTextField(
        leadingBitmap = item.icon,
        leadingIcon = R.drawable.ic_main_book,
        inputText = inputTitle,
        focusRequester = focusRequester
      )
      Spacer(modifier = Modifier.height(16.dp))
      RowItemTextField(leadingIcon = R.drawable.ic_main_link, inputText = inputUrl)
      Spacer(modifier = Modifier.height(16.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(MaterialTheme.colorScheme.surface)
          .clickable {
            scope.launch(ioAsyncExceptionHandler) {
              WebSiteDatabase.INSTANCE
                .websiteDao()
                .delete(webSiteInfo)
              onBack()
            }
          },
        contentAlignment = Center
      ) {
        Text(
          text = "删除",
          color = MaterialTheme.colorScheme.error,
          fontSize = 16.sp,
          fontWeight = FontWeight(400)
        )
      }
    }
  }
}

@Composable
fun RowItemTextField(
  leadingBitmap: ImageBitmap? = null,
  @DrawableRes leadingIcon: Int,
  inputText: MutableState<String>,
  focusRequester: FocusRequester? = null,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(50.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(MaterialTheme.colorScheme.surface),
    verticalAlignment = CenterVertically
  ) {
    val modifier = focusRequester?.let { Modifier.focusRequester(focusRequester) } ?: Modifier

    CustomTextField(
      value = inputText.value,
      onValueChange = { inputText.value = it },
      modifier = modifier,
      spacerWidth = 0.dp,
      leadingIcon = {
        leadingBitmap?.let {
          Image(
            bitmap = it,
            contentDescription = "Icon",
            modifier = Modifier
              .padding(horizontal = 12.dp, vertical = 11.dp)
              .size(28.dp)
          )
        } ?: run {
          Icon(
            imageVector = ImageVector.vectorResource(leadingIcon),
            contentDescription = "Icon",
            modifier = Modifier
              .padding(horizontal = 12.dp, vertical = 11.dp)
              .size(28.dp),
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    )
  }
}

/**
 * 三个标签页主界面
 */
@Composable
private fun PopTabRowContent(
  viewModel: BrowserViewModel,
  selectedTabIndex: MutableState<Int>,
  openBookManager: (WebSiteInfo) -> Unit
) {
  val popupViewState = remember { mutableStateOf(PopupViewState.Options) }
  val tabs = listOf(
    TabItem(R.string.browser_nav_option, R.drawable.ic_main_option, PopupViewState.Options),
    TabItem(R.string.browser_nav_book, R.drawable.ic_main_book, PopupViewState.BookList),
    TabItem(R.string.browser_nav_history, R.drawable.ic_main_history, PopupViewState.HistoryList),
  )

  LaunchedEffect(selectedTabIndex) {
    snapshotFlow { selectedTabIndex.value }.collect {
      if (it < tabs.size && it >= 0) {
        popupViewState.value = tabs[it].entry
      }
    }
  }

  Column {
    TabRow(
      selectedTabIndex = selectedTabIndex.value,
      containerColor = MaterialTheme.colorScheme.background,
      divider = {}
    ) {
      tabs.forEachIndexed { index, tabItem ->
        Tab(
          selected = selectedTabIndex.value == index,
          onClick = { selectedTabIndex.value = index },
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
    PopContentView(popupViewState, viewModel, openBookManager)
  }
}

// 显示具体内容部分，其中又可以分为三个部分类型，操作页，书签列表，历史列表
@Composable
private fun PopContentView(
  popupViewState: MutableState<PopupViewState>,
  viewModel: BrowserViewModel,
  openBookManager: (WebSiteInfo) -> Unit
) {
  val historyViewModel = remember { HistoryViewModel() }
  val bookViewModel = remember { BookViewModel() }
  val scope = rememberCoroutineScope()
  val bottomSheetModel = LocalModalBottomSheet.current

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    when (popupViewState.value) {
      PopupViewState.BookList -> BrowserListOfBook(bookViewModel,
        onOpenSetting = { openBookManager(it) },
        onSearch = {
          scope.launch {
            bottomSheetModel.hide()
            viewModel.handleIntent(BrowserIntent.SearchWebView(it))
          }
        }
      )

      PopupViewState.HistoryList -> BrowserListOfHistory(historyViewModel) {
        scope.launch {
          bottomSheetModel.hide()
          viewModel.handleIntent(BrowserIntent.SearchWebView(it))
        }
      }

      else -> PopContentOptionItem(viewModel)
    }
  }
}

@Composable
private fun PopContentOptionItem(viewModel: BrowserViewModel) {
  val scope = rememberCoroutineScope()
  val activity = LocalContext.current.findActivity()
  val bottomSheetModel = LocalModalBottomSheet.current
  val localCommonUrl = LocalCommonUrl.current
  // 判断权限
  val launcher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(),
      onResult = { isGranted ->
        //判断权限申请结果，并根据结果显示不同画面，由于 onResult 不是一个 @Composable lambda，所以不能直接显示 Composable 需要通过修改 state 等方式间接显示 Composable
        if (isGranted) {
          viewModel.handleIntent(BrowserIntent.ShareWebSiteInfo(activity))
        }
      })
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        RowItemMenuView(text = "添加书签", trailingIcon = R.drawable.ic_main_book) {
          viewModel.handleIntent(BrowserIntent.SaveBookWebSiteInfo)
        } // 添加书签

        Spacer(modifier = Modifier.height(12.dp))
        RowItemMenuView(text = "分享", trailingIcon = R.drawable.ic_main_share) {
          if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            viewModel.handleIntent(BrowserIntent.ShareWebSiteInfo(activity))
          } else {
            launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)/*请求权限*/
          }
        } // 分享

        Spacer(modifier = Modifier.height(12.dp))
        RowItemMenuView(text = "无痕浏览", trailingContent = { modifier ->
          Switch(
            modifier = modifier
              .padding(horizontal = 12.dp, vertical = 10.dp)
              .size(width = 50.dp, height = 30.dp),
            checked = viewModel.isNoTrace.value,
            onCheckedChange = { viewModel.saveBrowserMode(it) }
          )
        }) {} // 无痕浏览

        Spacer(modifier = Modifier.height(12.dp))
        RowItemMenuView(text = "隐私政策", trailingContent = { modifier ->
          Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_more),
            contentDescription = "Manager",
            modifier = modifier
              .padding(horizontal = 12.dp, vertical = 15.dp)
              .size(20.dp)
              .graphicsLayer(rotationZ = -90f),
            tint = MaterialTheme.colorScheme.outlineVariant
          )
        }) {
          scope.launch {
            bottomSheetModel.hide()
            localCommonUrl.value = PrivacyUrl
          }
        } // 隐私政策
      }
    }
  }
}

@Composable
private fun RowItemMenuView(
  text: String,
  @DrawableRes trailingIcon: Int? = null,
  trailingContent: (@Composable (Modifier) -> Unit)? = null,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(50.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(MaterialTheme.colorScheme.surface)
      .clickable { onClick() }
  ) {
    Text(
      text = text,
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.CenterStart)
        .padding(start = 16.dp, end = 52.dp),
      textAlign = TextAlign.Start,
      fontSize = 16.sp,
      fontWeight = FontWeight(400),
      color = MaterialTheme.colorScheme.onSurface
    )

    trailingIcon?.let { icon ->
      Icon(
        imageVector = ImageVector.vectorResource(icon),
        contentDescription = "Icon",
        modifier = Modifier
          .align(CenterEnd)
          .padding(horizontal = 12.dp, vertical = 11.dp)
          .size(28.dp),
        tint = MaterialTheme.colorScheme.onSurface
      )
    } ?: trailingContent?.let { view -> view(Modifier.align(CenterEnd)) }
  }
}

/**
 * 显示多视图窗口
 */
@Composable
internal fun BrowserMultiPopupView(viewModel: BrowserViewModel) {
  val browserViewList = viewModel.uiState.browserViewList

  AnimatedVisibility(visibleState = viewModel.uiState.multiViewShow) {
    BackHandler {
      viewModel.handleIntent(BrowserIntent.UpdateMultiViewState(false))
    }
    // 高斯模糊做背景
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
        viewModel.uiState.currentBrowserBaseView.value?.bitmap?.let { bitmap ->
          Image(
            bitmap = bitmap,
            contentDescription = "BackGround",
            alignment = TopStart,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
              .fillMaxSize()
              .blur(radius = 16.dp)
          )
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .clickable(enabled = false) {}
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
          .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = CenterVertically
      ) {
        Icon(
          imageVector = ImageVector.vectorResource(id = R.drawable.ic_main_add),
          contentDescription = "Add",
          modifier = Modifier
            .padding(start = 8.dp, end = 8.dp)
            .size(28.dp)
            .align(CenterVertically)
            .clickable { viewModel.handleIntent(BrowserIntent.AddNewMainView()) },
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
    Column(horizontalAlignment = CenterHorizontally) {
      Image(
        painter = browserBaseView.bitmap?.let {
          remember(it) {
            BitmapPainter(it, filterQuality = FilterQuality.Medium)
          }
        } ?: rememberVectorPainter(Icons.Default.BrokenImage),
        contentDescription = null,
        modifier = Modifier
          .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
          .size(width = sizeTriple.first, height = sizeTriple.second)
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
          .clickable {
            viewModel.handleIntent(BrowserIntent.UpdateMultiViewState(false, index))
          }
          .align(CenterHorizontally),
        contentScale = ContentScale.FillWidth, //ContentScale.FillBounds,
        alignment = if (browserBaseView is BrowserMainView) Center else TopStart
      )
      val contentPair = when (browserBaseView) {
        is BrowserMainView -> {
          Pair("起始页", BitmapUtil.decodeBitmapFromResource(R.drawable.ic_main_star))
        }

        is BrowserWebView -> {
          if (browserBaseView.viewItem.state.lastLoadedUrl?.isSystemUrl() == true) {
            Pair("起始页", BitmapUtil.decodeBitmapFromResource(R.drawable.ic_main_star))
          } else {
            Pair(browserBaseView.viewItem.state.pageTitle, browserBaseView.viewItem.state.pageIcon)
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
          Image(
            bitmap = imageBitmap, contentDescription = null, modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(2.dp))
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
      Image(
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