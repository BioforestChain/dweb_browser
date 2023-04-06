package info.bagen.rust.plaoc.ui.browser

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import info.bagen.rust.plaoc.R
import info.bagen.rust.plaoc.ui.theme.Blue
import info.bagen.rust.plaoc.ui.theme.DimenBottomBarHeight

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
  val localConfig = LocalConfiguration.current
  AnimatedVisibility(visibleState = viewModel.uiState.multiViewShow) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colors.primaryVariant)
        /*.clickable(indication = null,
          onClick = { },
          interactionSource = remember { MutableInteractionSource() })*/
    ) {
      if (browserViewList.size == 1) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          Image(
            bitmap = browserViewList[0].bitmap?.asImageBitmap()
              ?: ImageBitmap.imageResource(id = R.drawable.ic_launcher),
            contentDescription = null,
            modifier = Modifier
              .size(
                localConfig.screenWidthDp.dp * 3 / 5, localConfig.screenHeightDp.dp / 2
              )
              .align(Alignment.TopCenter)
          )
        }
      } else {
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f)) {
          items(browserViewList.size) {

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