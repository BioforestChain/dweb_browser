package info.bagen.dwebbrowser.microService.browser.jmm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.R
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.bookmark.clickableWithNoEffect
import org.dweb_browser.browserUI.download.DownLoadInfo
import org.dweb_browser.browserUI.download.DownLoadStatus
import org.dweb_browser.microservice.help.JmmAppInstallManifest
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import java.text.DecimalFormat

private val TopBarHeight = 44.dp
private val HeadHeight = 128.dp
private val AppInfoHeight = 88.dp
private val VerticalPadding = 16.dp
private val HorizontalPadding = 16.dp
private val ShapeCorner = 16.dp
private val HeadIconSize = 28.dp
private val AppBottomHeight = 82.dp
private val ImageWidth = 135.dp
private val ImageHeight = 240.dp

private data class PreviewState(
  val showPreview: MutableTransitionState<Boolean> = MutableTransitionState(false), // 用于判断是否显示预览界面
  val selectIndex: MutableState<Int> = mutableStateOf(0), // 用于保存当前选中的图片下标
  //val firstVisible: MutableState<Int> = mutableStateOf(0), // 用于记录第一个有效显示的照片
  //val firstVisibleOffset: MutableState<Int> = mutableStateOf(0), // 用于记录第一个有效显示的照片偏移量
  val offset: MutableState<Offset> = mutableStateOf(Offset.Zero), // 用于保存当前选中图片的中心坐标
  var imageLazy: LazyListState? = null,
  var outsideLazy: LazyListState,
  var screenWidth: Int,
  var screenHeight: Int,
  var statusBarHeight: Int,
  var density: Float,
)

@Composable
fun MALLBrowserView(viewModel: JmmManagerViewModel, onBack: () -> Unit) {
  val jmmMetadata = viewModel.uiState.jmmAppInstallManifest
  val topBarAlpha = remember { mutableStateOf(0f) }
  val lazyListState = rememberLazyListState()
  val screenWidth = LocalConfiguration.current.screenWidthDp
  val screenHeight = LocalConfiguration.current.screenHeightDp
  val density = LocalContext.current.resources.displayMetrics.density
  val statusBarHeight = WindowInsets.statusBars.getTop(Density(App.appContext))
  val previewState = remember {
    PreviewState(
      outsideLazy = lazyListState,
      screenWidth = screenWidth,
      screenHeight = screenHeight,
      statusBarHeight = statusBarHeight,
      density = density
    )
  }
  val firstHeightPx = HeadHeight.value * density / 2 // 头部item的高度是128.dp
  val scope = rememberCoroutineScope()

  LaunchedEffect(lazyListState) {
    snapshotFlow { lazyListState.firstVisibleItemScrollOffset }.collect {
      topBarAlpha.value = when (lazyListState.firstVisibleItemIndex) {
        0 -> if (it < firstHeightPx) {
          0f
        } else {
          (it - firstHeightPx) / firstHeightPx
        }

        else -> 1f
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      //.navigationBarsPadding()
  ) {
    AppInfoContentView(lazyListState, jmmMetadata) { index, imageLazyListState ->
      scope.launch {
        previewState.selectIndex.value = index
        previewState.imageLazy = imageLazyListState
        previewState.offset.value = measureCenterOffset(index, previewState)
        previewState.showPreview.targetState = true
      }
    }
    TopAppBar(topBarAlpha, jmmMetadata.name, onBack)
    BottomDownloadButton(viewModel.uiState.downloadInfo, jmmMetadata) {
      viewModel.handlerIntent(JmmIntent.ButtonFunction)
    }
    ImagePreview(jmmMetadata, previewState)
  }
}

private suspend fun measureCenterOffset(index: Int, previewState: PreviewState): Offset {
  val firstVisible = previewState.imageLazy?.firstVisibleItemIndex ?: 0
  val firstVisibleOffset = previewState.imageLazy?.firstVisibleItemScrollOffset ?: 0
  val density = previewState.density
  val statusBarHeight = previewState.statusBarHeight
  val screenWidth = previewState.screenWidth
  val screenHeight = previewState.screenHeight
  // 计算图片中心点的坐标
  val totalTop = when (previewState.outsideLazy.firstVisibleItemIndex) {
    0 -> { // 状态栏，顶部工具栏，头部栏，评分栏
      (TopBarHeight + HeadHeight + AppInfoHeight + ImageHeight / 2 + VerticalPadding).value * density + statusBarHeight
    }

    1 -> {
      (TopBarHeight + AppInfoHeight + ImageHeight / 2 + VerticalPadding).value * density + statusBarHeight
    }

    2 -> {
      (TopBarHeight + ImageHeight / 2 + VerticalPadding).value * density + statusBarHeight
    }

    else -> {
      statusBarHeight.toFloat()
    }
  }
  val realTop =
    (totalTop - previewState.outsideLazy.firstVisibleItemScrollOffset) / (screenHeight * density)

  val realLeft = if (index > firstVisible) {
    val left1 = (HorizontalPadding + ImageWidth).value * density - firstVisibleOffset // 第一格减去移动量
    val left2 =
      (index - firstVisible - 1) * (ImageWidth + HorizontalPadding).value * density // 中间间隔多少个图片
    val left3 = (ImageWidth / 2 + HorizontalPadding).value * density // 点击的图片本身
    (left1 + left2 + left3) / (screenWidth * density)
  } else {
    val left = (ImageWidth / 2 + HorizontalPadding).value * density - firstVisibleOffset
    left / (screenWidth * density)
  }
  return Offset(realLeft, realTop)
}

@Composable
private fun TopAppBar(alpha: MutableState<Float>, title: String, onBack: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface.copy(alpha.value))
      //.statusBarsPadding()
      .height(TopBarHeight),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = ImageVector.vectorResource(R.drawable.ic_main_back),
      contentDescription = "Back",
      modifier = Modifier
        .clickable { onBack() }
        .padding(horizontal = HorizontalPadding, vertical = VerticalPadding / 2)
        .size(HeadIconSize)
    )
    Text(
      text = title,
      fontWeight = FontWeight(500),
      fontSize = 18.sp,
      color = MaterialTheme.colorScheme.onSurface.copy(alpha.value)
    )
  }
}

@Composable
private fun BoxScope.BottomDownloadButton(
  downLoadInfo: MutableState<DownLoadInfo>, jmmMetadata: JmmAppInstallManifest, onClick: () -> Unit
) {
  val background = MaterialTheme.colorScheme.surface
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .align(Alignment.BottomCenter)
      .background(
        brush = Brush.verticalGradient(listOf(background.copy(0f), background))
      )
  ) {
    var showLinearProgress = false
    val text = when (downLoadInfo.value.downLoadStatus) {
      DownLoadStatus.IDLE, DownLoadStatus.CANCEL -> {
        "下载 (${jmmMetadata.bundle_size.toSpaceSize()})"
      }

      DownLoadStatus.NewVersion -> {
        "更新 (${jmmMetadata.bundle_size.toSpaceSize()})"
      }

      DownLoadStatus.DownLoading -> {
        showLinearProgress = true
        "下载中".displayDownLoad(downLoadInfo.value.size, downLoadInfo.value.dSize)
      }

      DownLoadStatus.PAUSE -> {
        showLinearProgress = true
        "暂停".displayDownLoad(downLoadInfo.value.size, downLoadInfo.value.dSize)
      }

      DownLoadStatus.DownLoadComplete -> "安装中..."
      DownLoadStatus.INSTALLED -> "打开"
      DownLoadStatus.FAIL -> "重新下载"
    }

    val modifier = Modifier
      .padding(horizontal = 64.dp, vertical = 16.dp)
      .shadow(elevation = 2.dp, shape = RoundedCornerShape(ShapeCorner))
      .fillMaxWidth()
      .height(50.dp)
    val m2 = if (showLinearProgress) {
      val percent =
        if (downLoadInfo.value.size == 0L) {
          0f
        } else {
          downLoadInfo.value.dSize * 1.0f / downLoadInfo.value.size
        }
      modifier.background(
        Brush.horizontalGradient(
          0.0f to MaterialTheme.colorScheme.primary,
          maxOf(percent - 0.02f, 0.0f) to MaterialTheme.colorScheme.primary,
          minOf(percent + 0.02f, 1.0f) to MaterialTheme.colorScheme.outlineVariant,
          1.0f to MaterialTheme.colorScheme.outlineVariant
        )
      )
    } else {
      modifier.background(MaterialTheme.colorScheme.primary)
    }

    Box(
      modifier = m2.clickableWithNoEffect { onClick()  },
      contentAlignment = Alignment.Center
    ) {
      Text(text = text, color = MaterialTheme.colorScheme.onPrimary)
    }
  }
}

@Composable
private fun AppInfoContentView(
  lazyListState: LazyListState, jmmAppInstallManifest: JmmAppInstallManifest, onSelectPic: (Int, LazyListState) -> Unit
) {
  LazyColumn(
    state = lazyListState,
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(top = TopBarHeight)
  ) {
    // 头部内容， HeadHeight 128.dp
    item { AppInfoHeadView(jmmAppInstallManifest) }
    // 应用信息， 88.dp
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(topStart = ShapeCorner, topEnd = ShapeCorner))
          .background(MaterialTheme.colorScheme.surface)
      ) {
        AppInfoLazyRow(jmmAppInstallManifest)
      }
    }
    // 上面padding 16.dp
    item {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.surface)
      ) {
        CustomerDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
        CaptureListView(jmmAppInstallManifest, onSelectPic)
      }
    }

    item {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.surface)
      ) {
        CustomerDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
        AppIntroductionView(jmmAppInstallManifest)
        CustomerDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
        NewVersionInfoView(jmmAppInstallManifest)
        CustomerDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
        OtherInfoView(jmmAppInstallManifest)
        Spacer(modifier = Modifier.height(AppBottomHeight))
      }
    }
  }
}

/**
 * 顶部的头像和应用名称
 */
@Composable
private fun AppInfoHeadView(jmmAppInstallManifest: JmmAppInstallManifest) {
  val size = HeadHeight - VerticalPadding * 2
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = HorizontalPadding, vertical = VerticalPadding),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AsyncImage(
      model = jmmAppInstallManifest.logo,
      contentDescription = "AppIcon",
      modifier = Modifier
        .size(size)
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surface)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .height(size)
    ) {
      Text(
        text = jmmAppInstallManifest.name,
        maxLines = 2,
        fontWeight = FontWeight(500),
        fontSize = 22.sp,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onBackground
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = jmmAppInstallManifest.short_name,
        maxLines = 1,
        color = MaterialTheme.colorScheme.outlineVariant,
        overflow = TextOverflow.Ellipsis,
        fontSize = 12.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = buildAnnotatedString {
          append("人工复检 · ")
          withStyle(style = SpanStyle(color = Color.Green)) { append("无广告") }
        },
        fontSize = 12.sp
      )
    }
  }
}

/**
 * 中间的横向数据
 */
@Composable
private fun AppInfoLazyRow(jmmAppInstallManifest: JmmAppInstallManifest) {
  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .height(AppInfoHeight),
    contentPadding = PaddingValues(HorizontalPadding),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    item { // 评分
      DoubleRowItem(first = "4.9 分", second = "999+ 评论")
    }
    item { // 安装次数
      DoubleRowItem(first = "9527 万", second = "次安装")
    }
    item { // 年龄限制
      DoubleRowItem(first = "18+", second = "年满 18 周岁")
    }
    item { // 大小
      DoubleRowItem(first = jmmAppInstallManifest.bundle_size.toSpaceSize(), second = "大小")
    }
  }
}

@Composable
private fun DoubleRowItem(first: String, second: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = first,
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.outline,
      maxLines = 1
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = second,
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.outlineVariant,
      maxLines = 1
    )
  }
}

/**
 * 应用介绍的图片展示部分
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureListView(jmmAppInstallManifest: JmmAppInstallManifest, onSelectPic: (Int, LazyListState) -> Unit) {
  jmmAppInstallManifest.images?.let { images ->
    val lazyListState = rememberLazyListState()
    LazyRow(
      modifier = Modifier.padding(vertical = VerticalPadding),
      state = lazyListState,
      contentPadding = PaddingValues(horizontal = HorizontalPadding)
    ) {
      itemsIndexed(images) { index, item ->
        Card(
          onClick = { onSelectPic(index, lazyListState) },
          modifier = Modifier
            .padding(end = 16.dp)
            .size(ImageWidth, ImageHeight)
        ) {
          AsyncImage(model = item, contentDescription = null)
        }
      }
    }
  }
}

/**
 * 应用介绍描述部分
 */
@Composable
private fun AppIntroductionView(jmmAppInstallManifest: JmmAppInstallManifest) {
  val expanded = remember { mutableStateOf(false) }
  Column(modifier = Modifier.padding(horizontal = HorizontalPadding, vertical = VerticalPadding)) {
    Text(
      text = "应用介绍",
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Box(modifier = Modifier
      .animateContentSize()
      .clickable { expanded.value = !expanded.value }) {
      Text(
        text = jmmAppInstallManifest.description?: "",
        maxLines = if (expanded.value) Int.MAX_VALUE else 2,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

/**
 * 应用新版本信息部分
 */
@Composable
private fun NewVersionInfoView(jmmAppInstallManifest: JmmAppInstallManifest) {
  val expanded = remember { mutableStateOf(false) }
  Column(modifier = Modifier.padding(horizontal = HorizontalPadding, vertical = VerticalPadding)) {
    Text(
      text = "更新日志",
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "版本 ${jmmAppInstallManifest.version}",
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.outline,
      modifier = Modifier.padding(vertical = 6.dp)
    )

    Box(modifier = Modifier
      .animateContentSize()
      .clickable { expanded.value = !expanded.value }) {
      Text(
        text = jmmAppInstallManifest.change_log,
        maxLines = if (expanded.value) Int.MAX_VALUE else 2,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

/**
 * 应用的其他相关内容
 */
@Composable
private fun OtherInfoView(jmmAppInstallManifest: JmmAppInstallManifest) {
  Column(modifier = Modifier.padding(horizontal = HorizontalPadding, vertical = VerticalPadding)) {
    Text(
      text = "信息",
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(HorizontalPadding))
    OtherItemView(type = "开发者", content = jmmAppInstallManifest.author?.toContent() ?: "me")
    OtherItemView(type = "大小", content = jmmAppInstallManifest.bundle_size.toSpaceSize())
    OtherItemView(type = "类别", content = jmmAppInstallManifest.categories.print())
    OtherItemView(type = "语言", content = "中文")
    OtherItemView(type = "年龄分级", content = "18+")
    OtherItemView(type = "版权", content = "@${jmmAppInstallManifest.author?.firstOrNull() ?: jmmAppInstallManifest.name}")
  }
}

fun MutableList<MICRO_MODULE_CATEGORY>.print() :String {
  val result = StringBuffer()
  this.forEach { category ->
    // 不要去掉，可能会出现null
    if (category !== null) {
      result.append(category.name)
    }
  }
  return result.toString()
}

/**
 * @param largeContent 该字段如果有数据，表示允许展开，查看详细信息
 */
@Composable
private fun OtherItemView(type: String, content: String, largeContent: String? = null) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = type, color = MaterialTheme.colorScheme.outline)

      Text(
        text = content,
        modifier = Modifier.weight(1f),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    CustomerDivider()
  }
}

@Composable
private fun CustomerDivider(modifier: Modifier = Modifier) =
  Divider(modifier = modifier, color = MaterialTheme.colorScheme.background)

/**
 * 图片预览图
 * @param visibleState 本来想使用 MutableTransitionState，但是后面发现进入和退出的聚焦点会动态变化，这样子就会导致这个组件每次都会重组，所以也可以直接改为 MutableState
 * @param select 当前查看的图片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImagePreview(jmmAppInstallManifest: JmmAppInstallManifest, previewState: PreviewState, ) {
  AnimatedVisibility(
    visibleState = previewState.showPreview,
    enter = scaleIn(
      initialScale = 0.3f,
      transformOrigin = TransformOrigin(previewState.offset.value.x, previewState.offset.value.y)
    ) + fadeIn(),
    exit = scaleOut(
      targetScale = 0.3f,
      transformOrigin = TransformOrigin(previewState.offset.value.x, previewState.offset.value.y)
    ) + fadeOut(),
  ) {
    BackHandler { previewState.showPreview.targetState = false }
    val pagerState = rememberPagerState(
      initialPage = previewState.selectIndex.value,
      initialPageOffsetFraction = 0f,
      pageCount = { jmmAppInstallManifest.images?.size ?: 0 }
    )
    val imageList = jmmAppInstallManifest.images ?: listOf()

    LaunchedEffect(previewState) { // 为了滑动图片后，刷新后端的图片中心点位置
      snapshotFlow { pagerState.currentPage }.collect { pager ->
        previewState.offset.value = measureCenterOffset(pager, previewState)
      }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
      HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState,
        pageSpacing = 0.dp,
        userScrollEnabled = true,
        reverseLayout = false,
        contentPadding = PaddingValues(0.dp),
        beyondBoundsPageCount = 0,
        pageContent = { index ->
          AsyncImage(
            model = imageList[index],
            contentDescription = "Picture",
            alignment = Alignment.Center,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
              .fillMaxSize()
              .clickableWithNoEffect { previewState.showPreview.targetState = false }
          )
        }
      )
      Row(
        Modifier
          .height(50.dp)
          .fillMaxWidth()
          .align(Alignment.BottomCenter),
        horizontalArrangement = Arrangement.Center
      ) {
        repeat(imageList.size) { iteration ->
          val color = if (pagerState.currentPage == iteration) Color.LightGray else Color.DarkGray
          Box(
            modifier = Modifier
              .padding(2.dp)
              .clip(CircleShape)
              .background(color)
              .size(8.dp)
          )
        }
      }
    }
  }
}

private fun String.displayDownLoad(total: Long, progress: Long): String {
  val GB = 1024 * 1024 * 1024 // 定义GB的计算常量
  val MB = 1024 * 1024 // 定义MB的计算常量
  val KB = 1024 // 定义KB的计算常量
  val df = DecimalFormat("0.0");//格式化小数
  val dValue: String
  val totalValue = if (total / GB >= 1) {
    dValue = df.format(progress.toFloat() / GB)
    df.format(total.toFloat() / GB) + " GB";
  } else if (total.toFloat() / MB >= 1) {
    dValue = df.format(progress.toFloat() / MB)
    df.format(total.toFloat() / MB) + " MB";
  } else if (total.toFloat() / KB >= 1) { //如果当前Byte的值大于等于1KB
    dValue = df.format(progress.toFloat() / KB)
    df.format(total.toFloat() / KB) + " KB";
  } else {
    dValue = "$progress"
    "$total B";
  }
  return if (dValue.isEmpty()) "$this ($totalValue)" else "$this ($dValue/$totalValue)"
}

private fun String.toSpaceSize(): String {
  if (this.isEmpty()) return "0"
  val size = this.toFloat()
  val GB = 1024 * 1024 * 1024 // 定义GB的计算常量
  val MB = 1024 * 1024 // 定义MB的计算常量
  val KB = 1024 // 定义KB的计算常量
  val df = DecimalFormat("0.0");//格式化小数
  return if (size / GB >= 1) {
    df.format(size / GB) + " GB";
  } else if (size / MB >= 1) {
    df.format(size / MB) + " MB";
  } else if (size / KB >= 1) { //如果当前Byte的值大于等于1KB
    df.format(size / KB) + " KB";
  } else {
    "$size B";
  }
}

fun List<String>.toContent(): String {
  val sb = StringBuffer()
  this.forEachIndexed { index, data ->
    if (index > 0) sb.append(", ")
    sb.append(data)
  }
  return sb.toString()
}