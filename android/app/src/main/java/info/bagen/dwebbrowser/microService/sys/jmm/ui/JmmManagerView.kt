package info.bagen.dwebbrowser.microService.sys.jmm.ui

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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import java.text.DecimalFormat

private val HeadHeight = 128.dp
private val VerticalPadding = 16.dp
private val HorizontalPadding = 16.dp
private val ShapeCorner = 16.dp
private val HeadIconSize = 28.dp
private val AppTopBarHeight = 44.dp
private val AppBottomHeight = 82.dp

@Composable
fun MALLBrowserView(viewModel: JmmManagerViewModel, onBack: () -> Unit) {
  val jmmMetadata = viewModel.uiState.downloadInfo.value.jmmMetadata
  val lazyListState = rememberLazyListState()
  val topBarAlpha = remember { mutableStateOf(0f) }
  val firstHeightPx =
    HeadHeight.value * LocalContext.current.resources.displayMetrics.density / 2 // 头部item的高度是128.dp
  val showPreview = remember { MutableTransitionState(false) }
  val selectImage = remember { mutableStateOf(0) }
  val offsetImage = remember { mutableStateOf(Offset.Zero) }

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
  ) {
    AppInfoContentView(lazyListState, jmmMetadata) { index, offset ->
      selectImage.value = index
      offsetImage.value = offset
      showPreview.targetState = true
    }
    TopAppBar(topBarAlpha, jmmMetadata.title, onBack)
    BottomDownloadButton(viewModel)
    ImagePreview(jmmMetadata, showPreview, selectImage, offsetImage)
  }
}

@Composable
private fun TopAppBar(alpha: MutableState<Float>, title: String, onBack: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface.copy(alpha.value))
      .statusBarsPadding()
      .height(AppTopBarHeight),
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
private fun BoxScope.BottomDownloadButton(viewModel: JmmManagerViewModel) {
  val background = MaterialTheme.colorScheme.surface
  val downLoadInfo = viewModel.uiState.downloadInfo.value
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .align(Alignment.BottomCenter)
      .background(
        brush = Brush.verticalGradient(listOf(background.copy(0f), background))
      )
  ) {
    var showLinearProgress = false
    val text = when (downLoadInfo.downLoadStatus) {
      DownLoadStatus.IDLE, DownLoadStatus.CANCEL -> {
        "下载 (${downLoadInfo.jmmMetadata.size.toSpaceSize()})"
      }

      DownLoadStatus.NewVersion -> {
        "更新 (${downLoadInfo.jmmMetadata.size.toSpaceSize()})"
      }

      DownLoadStatus.DownLoading -> {
        showLinearProgress = true
        "下载中".displayDownLoad(downLoadInfo.size, downLoadInfo.dSize)
      }

      DownLoadStatus.PAUSE -> {
        showLinearProgress = true
        "暂停".displayDownLoad(downLoadInfo.size, downLoadInfo.dSize)
      }

      DownLoadStatus.DownLoadComplete -> "安装中..."
      DownLoadStatus.INSTALLED -> "打开"
      DownLoadStatus.FAIL -> "重新下载"
    }

    val modifier = Modifier
      .padding(horizontal = 64.dp, vertical = 32.dp)
      .shadow(elevation = 2.dp, shape = RoundedCornerShape(ShapeCorner))
      .fillMaxWidth()
      .height(50.dp)
    val m2 = if (showLinearProgress) {
      val percent =
        if (downLoadInfo.size == 0L) 0f else downLoadInfo.dSize * 1.0f / downLoadInfo.size
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
      modifier = m2.clickable { viewModel.handlerIntent(JmmIntent.ButtonFunction) },
      contentAlignment = Alignment.Center
    ) {
      Text(text = text, color = MaterialTheme.colorScheme.onPrimary)
    }
  }
}

@Composable
private fun AppInfoContentView(
  lazyListState: LazyListState, jmmMetadata: JmmMetadata, onSelectPic: (Int, Offset) -> Unit
) {
  LazyColumn(
    state = lazyListState,
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(top = AppTopBarHeight)
  ) {
    item { AppInfoHeadView(jmmMetadata) }

    item {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(topStart = ShapeCorner, topEnd = ShapeCorner))
          .background(MaterialTheme.colorScheme.surface)
      ) {
        AppInfoLazyRow(jmmMetadata)
        CustomerDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
        CaptureListView(jmmMetadata, onSelectPic)
        CustomerDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
        AppIntroductionView(jmmMetadata)
        CustomerDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
        NewVersionInfoView(jmmMetadata)
        CustomerDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
        OtherInfoView(jmmMetadata)
        Spacer(modifier = Modifier.height(AppBottomHeight))
      }
    }
  }
}

/**
 * 顶部的头像和应用名称
 */
@Composable
private fun AppInfoHeadView(jmmMetadata: JmmMetadata) {
  val size = HeadHeight - VerticalPadding * 2
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = HorizontalPadding, vertical = VerticalPadding),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AsyncImage(
      model = jmmMetadata.icon,
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
        text = jmmMetadata.title,
        maxLines = 2,
        fontWeight = FontWeight(500),
        fontSize = 22.sp,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onBackground
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = jmmMetadata.subtitle,
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
private fun AppInfoLazyRow(jmmMetadata: JmmMetadata) {
  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = VerticalPadding),
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
      DoubleRowItem(first = jmmMetadata.size.toSpaceSize(), second = "大小")
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
private fun CaptureListView(jmmMetadata: JmmMetadata, onSelectPic: (Int, Offset) -> Unit) {
  jmmMetadata.images?.let { images ->
    val lazyListState = rememberLazyListState()
    LazyRow(
      modifier = Modifier.padding(vertical = VerticalPadding),
      state = lazyListState,
      contentPadding = PaddingValues(HorizontalPadding)
    ) {
      itemsIndexed(images) { index, item ->
        Card(
          onClick = { onSelectPic(index, Offset(0.5f, 0.5f)) }, modifier = Modifier
            .padding(end = 16.dp)
            .size(135.dp, 240.dp)
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
private fun AppIntroductionView(jmmMetadata: JmmMetadata) {
  val expanded = remember { mutableStateOf(false) }
  Column(modifier = Modifier.padding(horizontal = HorizontalPadding, vertical = VerticalPadding)) {
    Text(text = "应用介绍", fontSize = 18.sp, fontWeight = FontWeight.Bold)

    Box(modifier = Modifier
      .animateContentSize()
      .clickable { expanded.value = !expanded.value }) {
      Text(
        text = jmmMetadata.introduction,
        maxLines = if (expanded.value) Int.MAX_VALUE else 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * 应用新版本信息部分
 */
@Composable
private fun NewVersionInfoView(jmmMetadata: JmmMetadata) {
  val expanded = remember { mutableStateOf(false) }
  Column(modifier = Modifier.padding(horizontal = HorizontalPadding, vertical = VerticalPadding)) {
    Text(text = "新功能", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Text(
      text = "版本 ${jmmMetadata.version}",
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.outlineVariant,
      modifier = Modifier.padding(vertical = 6.dp)
    )

    Box(modifier = Modifier
      .animateContentSize()
      .clickable { expanded.value = !expanded.value }) {
      Text(
        text = "运用全新的功能，让使用更加安全便捷",
        maxLines = if (expanded.value) Int.MAX_VALUE else 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * 应用的其他相关内容
 */
@Composable
private fun OtherInfoView(jmmMetadata: JmmMetadata) {
  Column(modifier = Modifier.padding(horizontal = HorizontalPadding, vertical = VerticalPadding)) {
    Text(text = "信息", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(HorizontalPadding))
    OtherItemView(type = "开发者", content = jmmMetadata.author?.toContent() ?: "me")
    OtherItemView(type = "大小", content = jmmMetadata.size.toSpaceSize())
    OtherItemView(type = "类别", content = "娱乐")
    OtherItemView(type = "语言", content = "中文")
    OtherItemView(type = "年龄分级", content = "18+")
    OtherItemView(type = "版权", content = "bgwl")
  }
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
private fun ImagePreview(
  jmmMetadata: JmmMetadata,
  visibleState: MutableTransitionState<Boolean>,
  select: MutableState<Int>,
  offset: MutableState<Offset>
) {
  AnimatedVisibility(
    visibleState = visibleState,
    enter = scaleIn(transformOrigin = TransformOrigin(offset.value.x, offset.value.y)) + fadeIn(),
    exit = scaleOut(transformOrigin = TransformOrigin(offset.value.x, offset.value.y)) + fadeOut(),
  ) {
    BackHandler { visibleState.targetState = false }
    val pagerState = rememberPagerState(select.value)
    val imageList = jmmMetadata.images ?: listOf()
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
      HorizontalPager(
        pageCount = imageList.size,
        state = pagerState,
        modifier = Modifier.fillMaxSize()
      ) { index ->
        AsyncImage(
          model = imageList[index],
          contentDescription = "Picture",
          alignment = Alignment.Center,
          contentScale = ContentScale.FillWidth,
          modifier = Modifier
            .fillMaxSize()
            .clickable(indication = null,
              onClick = { visibleState.targetState = false },
              interactionSource = remember { MutableInteractionSource() }
            )
        )
      }
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
  var dValue: String
  val totalValue = if (total / GB >= 1) {
    dValue = df.format(1.0 * progress / GB)
    df.format(total / GB) + " GB";
  } else if (total / MB >= 1) {
    dValue = df.format(1.0 * progress / MB)
    df.format(total / MB) + " MB";
  } else if (total / KB >= 1) { //如果当前Byte的值大于等于1KB
    dValue = df.format(1.0 * progress / KB)
    df.format(total / KB) + " KB";
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