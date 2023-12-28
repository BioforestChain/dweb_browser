package org.dweb_browser.browser.web.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.data.WebSiteInfo
import org.dweb_browser.browser.web.data.WebSiteType
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserModel
import org.dweb_browser.browser.web.model.LocalModalBottomSheet
import org.dweb_browser.browser.web.model.PageType
import org.dweb_browser.browser.web.model.PopupViewState
import org.dweb_browser.browser.web.model.SheetState
import org.dweb_browser.browser.web.model.WebEngine
import org.dweb_browser.browser.web.model.toWebSiteInfo
import org.dweb_browser.helper.PrivacyUrl
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.platform.getCornerRadiusTop
import org.dweb_browser.helper.platform.rememberPureViewBox
import org.dweb_browser.helper.platform.theme.DimenBottomBarHeight
import org.dweb_browser.sys.window.render.NativeBackHandler

@Composable
internal fun BrowserBottomSheet(viewModel: BrowserViewModel) {
  val bottomSheetModel = LocalModalBottomSheet.current
  val scope = rememberCoroutineScope()

  if (bottomSheetModel.show.value) {
    NativeBackHandler {
      scope.launch {
        if (bottomSheetModel.state.value != SheetState.Hidden) {
          bottomSheetModel.hide()
        }
      }
    }

    val density = LocalDensity.current.density
    val topLeftRadius = getCornerRadiusTop(rememberPureViewBox(), density, 16f)
    BrowserModalBottomSheet(
      onDismissRequest = { scope.launch { bottomSheetModel.hide() } },
      shape = RoundedCornerShape(
        topStart = topLeftRadius * density,
        topEnd = topLeftRadius * density
      )
    ) {
      BrowserPopView(viewModel)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowserModalBottomSheet(
  onDismissRequest: () -> Unit,
  shape: Shape = BottomSheetDefaults.ExpandedShape,
  dragHandle: @Composable (() -> Unit) = { BottomSheetDefaults.DragHandle() },
  content: @Composable ColumnScope.() -> Unit,
) {
  val state = remember { mutableStateOf(SheetState.PartiallyExpanded) }
  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
  ) {
    key(state) {
      val density = LocalDensity.current.density
      val parentHeight = maxHeight.value * density
      val currentState = remember { mutableFloatStateOf(state.value.defaultHeight(parentHeight)) }

      val height = animateDpAsState(
        targetValue = (currentState.floatValue / density).dp, label = "",
        finishedListener = {
          if (state.value == SheetState.Hidden) {
            onDismissRequest()
          }
        }
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clickableWithNoEffect {
            currentState.floatValue = 0f
            state.value = SheetState.Hidden
          }
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(height.value)
          .align(Alignment.BottomCenter)
          .clip(shape)
          .background(MaterialTheme.colorScheme.background)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.CenterHorizontally)
            .pointerInput(currentState) {
              detectDragGestures(
                onDragEnd = {
                  if (currentState.floatValue > parentHeight * 3 / 4) {
                    currentState.floatValue = parentHeight
                    state.value = SheetState.Expanded
                  } else if (currentState.floatValue < parentHeight / 2) {
                    currentState.floatValue = 0f
                    state.value = SheetState.Hidden
                  } else {
                    currentState.floatValue = parentHeight * 2 / 3
                    state.value = SheetState.PartiallyExpanded
                  }
                },
                onDrag = { _, dragAmount ->
                  currentState.floatValue -= dragAmount.y
                }
              )
            }, contentAlignment = TopCenter
        ) {
          dragHandle()
        }
        content()
      }
    }

  }
}

/**
 * 弹出主界面，包括了三个tab和一个书签管理界面 TODO 目前缺少切换到书签管理界面后的展开问题
 */
@Composable
internal fun BrowserPopView(viewModel: BrowserViewModel) {
  val selectedTabIndex = LocalModalBottomSheet.current.tabIndex
  val pageIndex = LocalModalBottomSheet.current.pageType
  val webSiteInfo = LocalModalBottomSheet.current.webSiteInfo
  val webEngine = remember { mutableStateOf<WebEngine?>(null) }

  AnimatedContent(targetState = pageIndex, label = "",
    transitionSpec = {
      if (targetState.value.ordinal > initialState.value.ordinal) {
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
    when (targetPage.value) {
      PageType.Home -> {
        PopTabRowContent(
          viewModel = viewModel,
          selectedTabIndex = selectedTabIndex,
          openBookManager = { webSiteInfo.value = it; pageIndex.value = PageType.BookManager },
          openEngineManger = { pageIndex.value = PageType.EngineList }
        )
      }

      PageType.BookManager -> {
        PopBookManagerView(viewModel) { pageIndex.value = PageType.Home }
      }

      PageType.EngineList -> {
        PopSearchEngineListView(
          viewModel = viewModel,
          openEngineManger = { webEngine.value = it; pageIndex.value = PageType.EngineManger },
          onBack = { pageIndex.value = PageType.Home }
        )
      }

      PageType.EngineManger -> {
        PopEngineManagerView(webEngine) { pageIndex.value = PageType.EngineList }
      }
    }
  }
}

/**
 * 配置搜索引擎
 */
@Composable
private fun PopSearchEngineListView(
  viewModel: BrowserViewModel, openEngineManger: (WebEngine) -> Unit, onBack: () -> Unit
) {
  Column {
    ManagerTitleView(title = BrowserI18nResource.browser_options_engine_list(), onBack = onBack)

    /*Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
      verticalAlignment = CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Text(
        text = BrowserI18nResource.browser_engine_tag_search(),
        modifier = Modifier.width(100.dp)
      )
      Text(
        text = BrowserI18nResource.browser_engine_tag_host(),
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center
      )
      Box(modifier = Modifier.width(32.dp))
    }*/

    LazyColumn {
      itemsIndexed(viewModel.getSearchEngines()) { index, searchEngine ->
        Row(
          modifier = Modifier.clickable { /*openEngineManger(searchEngine)*/ }
            .fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
          verticalAlignment = CenterVertically
        ) {
          Text(text = searchEngine.name, modifier = Modifier.width(100.dp))
          Text(text = searchEngine.host, modifier = Modifier.weight(1f))
          Box(modifier = Modifier.width(32.dp)) {
            Checkbox(
              checked = searchEngine.checked,
              onCheckedChange = {
                searchEngine.checked = it
                viewModel.updateSearchEngine(searchEngine)
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PopEngineManagerView(webEngine: MutableState<WebEngine?>, onBack: () -> Unit) {
  val currentEngine = webEngine.value?.let {
    remember { mutableStateOf(WebEngine(it.name, it.host, it.start)) }
  } ?: return
  val browserViewModel = LocalBrowserModel.current
  Column {
    ManagerTitleView(
      title = BrowserI18nResource.browser_options_engine_update(),
      onBack = onBack,
      onDone = {
        webEngine.value?.apply {
          name = currentEngine.value.name
          start = currentEngine.value.start
          browserViewModel.updateSearchEngine(this)
        }
      }
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
      Text(BrowserI18nResource.browser_engine_tag_search())
      OutlinedTextField(
        value = currentEngine.value.name,
        onValueChange = { currentEngine.value.name = it },
        maxLines = 1,
        modifier = Modifier.fillMaxWidth()
      )
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
      Text(BrowserI18nResource.browser_engine_tag_host())
      OutlinedTextField(
        value = currentEngine.value.host,
        maxLines = 1,
        enabled = false,
        onValueChange = {},
        modifier = Modifier.fillMaxWidth()
      )
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
      Text(BrowserI18nResource.browser_engine_tag_url())
      OutlinedTextField(
        value = currentEngine.value.start,
        onValueChange = { currentEngine.value.start = it },
        maxLines = 1,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

@Composable
private fun ManagerTitleView(
  title: String, onBack: () -> Unit, onDone: (() -> Unit)? = null
) {
  Row(
    modifier = Modifier.fillMaxWidth().height(44.dp),
    verticalAlignment = CenterVertically
  ) {
    Icon(
      imageVector = Icons.Default.ArrowBack,// ImageVector.vectorResource(R.drawable.ic_main_back),
      contentDescription = "Back",
      modifier = Modifier.clickable { onBack() }.padding(horizontal = 16.dp).size(24.dp),
      tint = MaterialTheme.colorScheme.onBackground
    )
    Text(
      text = title,
      modifier = Modifier.weight(1f),
      textAlign = TextAlign.Center,
      fontSize = 18.sp
    )
    Box(
      modifier = Modifier.clickable { onDone?.let { it() } }
        .padding(horizontal = 16.dp).width(48.dp),
      contentAlignment = Center
    ) {
      onDone?.let {
        Text(
          text = BrowserI18nResource.browser_options_store(),
          color = MaterialTheme.colorScheme.primary,
          fontSize = 18.sp
        )
      }
    }
  }
}

/**
 * 书签管理界面
 */
@Composable
private fun PopBookManagerView(viewModel: BrowserViewModel, onBack: () -> Unit) {
  val webSiteInfo = LocalModalBottomSheet.current.webSiteInfo
  val inputTitle = remember { mutableStateOf(webSiteInfo.value?.title ?: "") }
  val inputUrl = remember { mutableStateOf(webSiteInfo.value?.url ?: "") }
  Column(modifier = Modifier.fillMaxSize()) {
    ManagerTitleView(
      title = BrowserI18nResource.browser_options_editBook(),
      onBack = onBack,
      onDone = {
        webSiteInfo.value?.apply {
          title = inputTitle.value
          url = inputUrl.value
          viewModel.updateBookLink(this)
          onBack()
        }
      }
    )
    val item = webSiteInfo.value ?: return
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
      delay(100)
      focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
      RowItemTextField(
        leadingBitmap = item.iconImage,
        leadingIcon = Icons.Default.Book,
        inputText = inputTitle,
        focusRequester = focusRequester
      )
      Spacer(modifier = Modifier.height(16.dp))
      RowItemTextField(leadingIcon = Icons.Default.Link, inputText = inputUrl)
      Spacer(modifier = Modifier.height(16.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .clickable {
            viewModel.removeBookLink(item)
            onBack()
          },
        contentAlignment = Center
      ) {
        Text(
          text = BrowserI18nResource.browser_options_delete(),
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
  leadingIcon: ImageVector,
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
            imageVector = leadingIcon,
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
  selectedTabIndex: MutableState<PopupViewState>,
  openBookManager: (WebSiteInfo) -> Unit,
  openEngineManger: () -> Unit,
) {
  val popupViewState = remember { mutableStateOf(PopupViewState.Options) }

  LaunchedEffect(selectedTabIndex) {
    snapshotFlow { selectedTabIndex.value }.collect {
      popupViewState.value = it
    }
  }

  Column {
    TabRow(
      selectedTabIndex = selectedTabIndex.value.index,
      containerColor = MaterialTheme.colorScheme.background,
      divider = {}
    ) {
      PopupViewState.values().forEachIndexed { index, tabItem ->
        Tab(
          selected = selectedTabIndex.value == tabItem,
          onClick = { selectedTabIndex.value = tabItem },
          icon = {
            Icon(
              imageVector = tabItem.imageVector,
              contentDescription = tabItem.title,
              modifier = Modifier.size(24.dp)
            )
          },
        )
      }
    }
    PopContentView(popupViewState, viewModel, openBookManager, openEngineManger)
  }
}

// 显示具体内容部分，其中又可以分为三个部分类型，操作页，书签列表，历史列表
@Composable
private fun PopContentView(
  popupViewState: MutableState<PopupViewState>,
  viewModel: BrowserViewModel,
  openBookManager: (WebSiteInfo) -> Unit,
  openEngineManger: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val bottomSheetModel = LocalModalBottomSheet.current

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    when (popupViewState.value) {
      PopupViewState.BookList -> BrowserListOfBook(viewModel,
        onOpenSetting = { openBookManager(it) },
        onSearch = {
          scope.launch {
            viewModel.searchWebView(it)
            bottomSheetModel.hide()
          }
        }
      )

      PopupViewState.HistoryList -> BrowserListOfHistory(viewModel) {
        scope.launch {
          viewModel.searchWebView(it)
          bottomSheetModel.hide()
        }
      }

      else -> PopContentOptionItem(viewModel) { openEngineManger() }
    }
  }
}

@Composable
private fun PopContentOptionItem(viewModel: BrowserViewModel, openEngineManage: () -> Unit) {
  val scope = rememberCoroutineScope()
  val bottomSheetModel = LocalModalBottomSheet.current
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        val contentWebItem = viewModel.currentTab?.contentWebItem?.value
        // 添加书签
        RowItemMenuView(
          text = BrowserI18nResource.browser_options_addToBook(), // stringResource(id = R.string.browser_options_book),
          enable = contentWebItem != null,
          trailingIcon = Icons.Default.Book
        ) {
          viewModel.currentTab?.contentWebItem?.value?.viewItem?.let { viewItem ->
            scope.launch {
              viewItem.webView.toWebSiteInfo(WebSiteType.Book)?.let {
                viewModel.addBookLink(it)
              } ?: run {
                viewModel.showToastMessage(BrowserI18nResource.toast_message_add_book_invalid.text)
              }
            }
          } ?: viewModel.showToastMessage(BrowserI18nResource.toast_message_add_book_invalid.text)
        }

        // 分享
        Spacer(modifier = Modifier.height(12.dp))
        RowItemMenuView(
          text = BrowserI18nResource.browser_options_share(),
          enable = contentWebItem != null,
          trailingIcon = Icons.Default.Share
        ) {
          scope.launch { viewModel.shareWebSiteInfo() }
        }

        // 无痕浏览
        Spacer(modifier = Modifier.height(12.dp))
        RowItemMenuView(
          text = BrowserI18nResource.browser_options_noTrace(),
          trailingContent = { modifier ->
            Switch(
              modifier = modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .size(width = 50.dp, height = 30.dp),
              checked = viewModel.isNoTrace.value,
              onCheckedChange = {
                scope.launch { viewModel.saveBrowserMode(it) }
              }
            )
          }) {}

        // 隐私政策
        Spacer(modifier = Modifier.height(12.dp))
        RowItemMenuView(
          text = BrowserI18nResource.browser_options_privacy(), // stringResource(id = R.string.browser_options_privacy),
          trailingContent = { modifier ->
            Icon(
              imageVector = Icons.Default.ExpandMore, // ImageVector.vectorResource(R.drawable.ic_more),
              contentDescription = "Manager",
              modifier = modifier
                .padding(horizontal = 12.dp, vertical = 15.dp)
                .size(20.dp)
                .graphicsLayer(rotationZ = -90f),
              tint = MaterialTheme.colorScheme.outlineVariant
            )
          }
        ) {
          scope.launch {
            viewModel.searchWebView(PrivacyUrl)
            bottomSheetModel.hide()
          }
        }

        // 搜索引擎
        Spacer(modifier = Modifier.height(12.dp))
        RowItemMenuView(
          text = BrowserI18nResource.browser_options_search_engine(),
          trailingIcon = Icons.Default.Settings
        ) { openEngineManage() }
      }
    }
  }
}

@Composable
private fun RowItemMenuView(
  text: String,
  enable: Boolean = true,
  trailingIcon: ImageVector? = null,
  trailingContent: (@Composable (Modifier) -> Unit)? = null,
  onClick: () -> Unit
) {
  if (!enable) return
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
        imageVector = icon,
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
  val scope = rememberCoroutineScope()
  AnimatedVisibility(visibleState = viewModel.showMultiView) {
    NativeBackHandler {
      scope.launch {
        if (viewModel.showMultiView.targetState) {
          viewModel.updateMultiViewState(false)
        }
      }
    }
    // 高斯模糊做背景
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
      //if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
      viewModel.currentTab?.bitmap?.let { bitmap ->
        Image(
          bitmap = bitmap,
          contentDescription = "BackGround",
          alignment = TopStart,
          contentScale = ContentScale.FillWidth,
          modifier = Modifier
            .fillMaxSize()
            .blur(radius = 16.dp)
        )
        //}
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .clickable(enabled = false) {}
    ) {
      if (viewModel.listSize == 1) {
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
            MultiItemView(viewModel, viewModel.getBrowserViewOrNull(0)!!, true)
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
          items(viewModel.listSize) {
            MultiItemView(viewModel, viewModel.getBrowserViewOrNull(it)!!, index = it)
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
          imageVector = Icons.Default.Add, // ImageVector.vectorResource(id = R.drawable.ic_main_add),
          contentDescription = "Add",
          modifier = Modifier
            .padding(start = 8.dp, end = 8.dp)
            .size(28.dp)
            .align(CenterVertically)
            .clickable { scope.launch { viewModel.addNewMainView() } },
          tint = MaterialTheme.colorScheme.primary,
        )
        val content = BrowserI18nResource.browser_multi_count()
        Text(
          text = "${viewModel.listSize} $content",
          modifier = Modifier.weight(1f),
          textAlign = TextAlign.Center
        )
        val done = BrowserI18nResource.browser_multi_done()
        Text(
          text = done,
          modifier = Modifier
            .padding(start = 8.dp, end = 8.dp)
            .clickable { scope.launch { viewModel.updateMultiViewState(false) } },
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

