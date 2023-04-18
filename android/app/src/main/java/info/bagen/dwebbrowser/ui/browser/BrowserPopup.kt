package info.bagen.dwebbrowser.ui.browser

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.ui.theme.Blue
import info.bagen.dwebbrowser.ui.theme.DimenBottomBarHeight
import info.bagen.dwebbrowser.util.BitmapUtil

@Composable
fun BoxScope.BrowserPopView(viewModel: BrowserViewModel) {
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp
  val localHeight = viewModel.uiState.popupViewState.value.getLocalHeight(screenHeight)
  val maxHeight = (screenHeight.value * 0.9f).dp - localHeight
  var offset by remember { mutableStateOf(Offset.Zero) }
  if (viewModel.uiState.popupViewState.value != PopupViewSate.NULL) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.LightGray.copy(0.3f))
        .clickable(indication = null,
          onClick = { viewModel.handleIntent(BrowserIntent.UpdatePopupViewState(PopupViewSate.NULL)) },
          interactionSource = remember { MutableInteractionSource() })
    )
    Box(modifier = Modifier
      .fillMaxWidth()
      .height(
        localHeight - if (offset.y >= 0) 0.dp
        else if (offset.y <= -maxHeight.value) -maxHeight
        else offset.y.dp
      )
      .align(Alignment.BottomCenter)
      .pointerInput(Unit) {
        detectDragGestures(onDragEnd = {
          offset = if (offset.y < -maxHeight.value / 2) {
            Offset(0f, -maxHeight.value)
          } else {
            Offset(0f, 0f)
          }
        }, onDrag = { _: PointerInputChange, dragAmount: Offset -> // 拖动中
          val curHeight = localHeight.value - (offset + dragAmount).y
          if (curHeight > screenHeight.value * 0.9f || curHeight < localHeight.value) {
            return@detectDragGestures
          }
          offset += dragAmount
        })
      }
      .clickable(indication = null,
        onClick = { /* TODO 这边不做响应 */ },
        interactionSource = remember { MutableInteractionSource() })
      .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .background(Color.White)
    ) {
      when (viewModel.uiState.popupViewState.value) {
        PopupViewSate.Options -> {
          BrowserOptionView(viewModel)
        }
        PopupViewSate.BookList -> {}
        PopupViewSate.Share -> {}
        else -> {}
      }
    }
  }
}

@Composable
fun BrowserOptionView(viewModel: BrowserViewModel) {
  AnimatedVisibility(
    visible = viewModel.uiState.popupViewState.value == PopupViewSate.Options,
    enter = slideInVertically { initialOffsetY -> initialOffsetY },
    exit = slideOutVertically { targetOffsetY -> targetOffsetY },
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        .background(MaterialTheme.colors.primary)
    ) {
      ButtonView(
        drawId = R.drawable.ic_main_book, name = stringResource(id = R.string.browser_nav_book)
      ) { }
      ButtonView(
        drawId = R.drawable.ic_main_history,
        name = stringResource(id = R.string.browser_nav_history)
      ) { }
      ButtonView(
        drawId = R.drawable.ic_main_share, name = stringResource(id = R.string.browser_nav_share)
      ) { }
    }
  }
}

@Composable
private fun RowScope.ButtonView(@DrawableRes drawId: Int, name: String, click: () -> Unit) {
  Box(
    modifier = Modifier
      .weight(1f)
      .fillMaxSize()
  ) {
    Column(modifier = Modifier.align(Alignment.Center)) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .background(Color.White)
      ) {
        Icon(
          imageVector = ImageVector.vectorResource(id = drawId),
          contentDescription = name,
          modifier = Modifier
            .size(30.dp)
            .align(Alignment.Center)
        )
      }
      Text(text = name)
    }
  }
}

@Composable
fun BrowserMultiPopupView(viewModel: BrowserViewModel) {
  val browserViewList = viewModel.uiState.browserViewList
  AnimatedVisibility(visibleState = viewModel.uiState.multiViewShow) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colors.primaryVariant)
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
          .background(MaterialTheme.colors.primary)
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
        bitmap = browserBaseView.bitmap ?: ImageBitmap.imageResource(id = R.drawable.ic_launcher),
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
          Icon(bitmap = imageBitmap, contentDescription = null, modifier = Modifier.size(12.dp))
        }
        Text(text = contentPair.first ?: "无标题", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
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