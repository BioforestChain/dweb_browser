package info.bagen.rust.plaoc.ui.browser

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.bagen.rust.plaoc.R

@Composable
fun BrowserPopView(viewModel: BrowserViewModel) {
  if (viewModel.uiState.popupViewState.value != PopupViewSate.NULL) {
    Box(modifier = Modifier
      .fillMaxSize()
      .background(Color.LightGray.copy(0.2f)))
  }
  Box {
    BrowserOptionView(viewModel = viewModel)
  }
}

@Composable
fun BoxScope.BrowserOptionView(viewModel: BrowserViewModel) {
  AnimatedVisibility(
    visible = viewModel.uiState.popupViewState.value == PopupViewSate.Options,
    enter = slideInVertically { initialOffsetY -> initialOffsetY },
    exit = slideOutVertically { targetOffsetY -> targetOffsetY },
    modifier = Modifier.align(Alignment.BottomCenter)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        .background(MaterialTheme.colors.primary)
    ) {
      ButtonView(
        drawId = R.drawable.ic_main_book,
        name = stringResource(id = R.string.browser_nav_book)
      ) { }
      ButtonView(
        drawId = R.drawable.ic_main_history,
        name = stringResource(id = R.string.browser_nav_history)
      ) { }
      ButtonView(
        drawId = R.drawable.ic_main_share,
        name = stringResource(id = R.string.browser_nav_share)
      ) { }
    }
  }
}

@Composable
private fun RowScope.ButtonView(@DrawableRes drawId: Int, name: String, click: () -> Unit) {
  Box(modifier = Modifier
    .weight(1f)
    .fillMaxSize()) {
    Column(modifier = Modifier.align(Alignment.Center)) {
      Box(modifier = Modifier
        .size(40.dp)
        .background(Color.White)) {
        Icon(
          bitmap = ImageBitmap.imageResource(id = drawId),
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