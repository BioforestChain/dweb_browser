package info.bagen.dwebbrowser.ui.browser.ios

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.ui.entity.BrowserBaseView
import info.bagen.dwebbrowser.ui.entity.BrowserMainView
import info.bagen.dwebbrowser.ui.entity.BrowserWebView
import info.bagen.dwebbrowser.ui.entity.PopupViewSate
import info.bagen.dwebbrowser.ui.theme.Blue
import info.bagen.dwebbrowser.ui.theme.DimenBottomBarHeight
import info.bagen.dwebbrowser.util.BitmapUtil

private val screenHeight: Dp
    @Composable get() {
        return LocalConfiguration.current.screenHeightDp.dp
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowserPopView(viewModel: BrowserViewModel) {
//    val scope = rememberCoroutineScope()

//    BackHandler(enabled = viewModel.uiState.modalBottomSheetState.isVisible) {
//        scope.launch { viewModel.uiState.modalBottomSheetState.hide() }
//    }

    val sheetState =
        rememberModalBottomSheetState()
    var openBottomSheet by remember { mutableStateOf(true) }

    if (openBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                openBottomSheet = false
            },
            sheetState = sheetState,// viewModel.uiState.modalBottomSheetState,
//        shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp),
        ) {

            class TabItem(
                @StringRes val title_res: Int,
                @DrawableRes val icon_res: Int,
                val entry: PopupViewSate
            ) {
                val title @Composable get() = stringResource(id = title_res)
                val icon @Composable get() = ImageVector.vectorResource(id = icon_res)

            };
            val tabs = listOf<TabItem>(
                TabItem(
                    R.string.browser_nav_option,
                    R.drawable.ic_main_option,
                    PopupViewSate.Options
                ),
                TabItem(R.string.browser_nav_book, R.drawable.ic_main_book, PopupViewSate.BookList),
                TabItem(
                    R.string.browser_nav_history,
                    R.drawable.ic_main_history,
                    PopupViewSate.HistoryList
                ),
            );
            var selectedTabIndex by remember {
                mutableStateOf(0)
            }
            var popupViewSate by viewModel.uiState.popupViewState;
            LaunchedEffect(selectedTabIndex) {
                snapshotFlow { selectedTabIndex }.collect {
                    popupViewSate = tabs[it].entry
                }
            }

            Column {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, tabItem ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index; },
                            icon = {
                                Icon(
                                    imageVector = tabItem.icon,
                                    contentDescription = tabItem.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
//                            text = {
//                                Text(text = title)
//                            }
                        )
                    }
                }
                PopContentView(viewModel) // 显示具体内容部分，其中又可以分为三个部分类型，操作页，书签列表，历史列表
            }

//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(screenHeight - 20.dp)
//                    .padding(horizontal = 10.dp)
//            ) {
//                PopTitleView(viewModel)// 显示标题部分
            Column {
                Text(text = tabs[selectedTabIndex].title)
                PopNavigatorView(viewModel)
            }// 显示导航
//                PopContentView(viewModel) // 显示具体内容部分，其中又可以分为三个部分类型，操作页，书签列表，历史列表
//            }
        }
    }
}


/**
 * 显示导航
 */
@Composable
private fun PopNavigatorView(viewModel: BrowserViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.LightGray)
            .padding(horizontal = 2.dp), verticalAlignment = CenterVertically
    ) {
        PopNavigatorItem(
            viewModel, R.drawable.ic_main_option, R.string.browser_nav_option, PopupViewSate.Options
        )
        PopNavigatorDiv(viewModel, PopupViewSate.Options, PopupViewSate.BookList) // 竖线
        PopNavigatorItem(
            viewModel, R.drawable.ic_main_book, R.string.browser_nav_book, PopupViewSate.BookList
        )
        PopNavigatorDiv(viewModel, PopupViewSate.BookList, PopupViewSate.HistoryList) // 竖线
        PopNavigatorItem(
            viewModel,
            R.drawable.ic_main_history,
            R.string.browser_nav_history,
            PopupViewSate.HistoryList
        )
    }
}

// 显示导航--导航项
@Composable
private fun RowScope.PopNavigatorItem(
    viewModel: BrowserViewModel,
    @DrawableRes drawId: Int,
    @StringRes stringId: Int,
    state: PopupViewSate
) {
    val type = viewModel.uiState.popupViewState.value
    Box(
        modifier = Modifier
            .weight(1f)
            .height(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (type == state) Color.White else Color.LightGray
            )
            .clickable(indication = null,
                onClick = { viewModel.handleIntent(BrowserIntent.UpdatePopupViewState(state)) },
                interactionSource = remember { MutableInteractionSource() })
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = drawId),
            contentDescription = stringResource(id = stringId),
            modifier = Modifier.align(Center)
        )
    }
}

/**
 * 竖线判断是否显示，如果当前的state不是下面参数范围，就显示，如果属于两个参数就不显示
 * @param left 左边的类型
 * @param right 右边的类型
 */
@Composable
private fun PopNavigatorDiv(
    viewModel: BrowserViewModel, left: PopupViewSate, right: PopupViewSate
) {
    val state = viewModel.uiState.popupViewState.value
    Spacer(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(
                if (state == left || state == right) {
                    Color.LightGray
                } else {
                    Color.Gray
                }
            )
    )
}

// 显示具体内容部分，其中又可以分为三个部分类型，操作页，书签列表，历史列表
@Composable
private fun PopContentView(viewModel: BrowserViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp)
    ) {
        when (viewModel.uiState.popupViewState.value) {
            PopupViewSate.BookList -> PopContentBookListItem(viewModel)
            PopupViewSate.HistoryList -> PopContentHistoryListItem(viewModel)
            else -> PopContentOptionItem(viewModel)
        }
    }
}

@Composable
private fun BoxScope.PopContentOptionItem(viewModel: BrowserViewModel) {
    Text(
        text = "无操作项", modifier = Modifier
            .align(TopCenter)
            .padding(top = screenHeight / 5)
    )
}

@Composable
private fun BoxScope.PopContentBookListItem(viewModel: BrowserViewModel) {
    Text(
        text = "书签列表", modifier = Modifier
            .align(TopCenter)
            .padding(top = screenHeight / 5)
    )
}

@Composable
private fun BoxScope.PopContentHistoryListItem(viewModel: BrowserViewModel) {
    Text(
        text = "历史记录", modifier = Modifier
            .align(TopCenter)
            .padding(top = screenHeight / 5)
    )
}

@Composable
internal fun BrowserMultiPopupView(viewModel: BrowserViewModel) {
    val browserViewList = viewModel.uiState.browserViewList
    AnimatedVisibility(visibleState = viewModel.uiState.multiViewShow) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
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
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_main_add),
                    contentDescription = "Add",
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .size(32.dp)
                        .align(CenterVertically),
                    tint = Blue,
                )
                Text(
                    text = "${browserViewList.size}个标签页",
                    modifier = Modifier
                        .weight(1f)
                        .align(CenterVertically),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "完成",
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .align(CenterVertically)
                        .clickable { viewModel.handleIntent(BrowserIntent.UpdateMultiViewState(false)) },
                    color = Blue
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
            Image(
                bitmap = browserBaseView.bitmap
                    ?: ImageBitmap.imageResource(id = R.drawable.ic_launcher),
                contentDescription = null,
                modifier = Modifier
                    .size(width = sizeTriple.first, height = sizeTriple.second)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .align(CenterHorizontally),
                contentScale = ContentScale.FillWidth, //ContentScale.FillBounds,
                alignment = TopStart
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

        if (!(onlyOne || browserBaseView is BrowserMainView)) {
            Box(modifier = Modifier
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.White)
                .align(Alignment.TopEnd)
                .clickable {
                    viewModel.handleIntent(BrowserIntent.RemoveBaseView(index))
                }) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_main_close),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.Gray
                )
            }
        }
    }
}