package org.dweb_browser.browser.web.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.dweb_browser.browser.common.AsyncImage
import org.dweb_browser.browser.common.CaptureParams
import org.dweb_browser.browser.common.CaptureView
import org.dweb_browser.browser.web.model.BrowserMainView
import org.dweb_browser.browser.web.ui.model.BrowserViewModel

@Composable
internal fun BrowserMainView(viewModel: BrowserViewModel, browserMainView: BrowserMainView) {
  val lazyListState = rememberLazyListState()
  LaunchedEffect(lazyListState) {
    delay(100)
    snapshotFlow { lazyListState.isScrollInProgress }.collect { scroll ->
      if (!scroll) {
        delay(200); browserMainView.controller.capture(CaptureParams.Normal)
      }
    }
  }

  CaptureView(
    controller = browserMainView.controller,
    onCaptured = { imageBitmap, _ ->
      imageBitmap?.let { bitmap ->
        viewModel.currentTab?.bitmap = bitmap
      }
    }) {
    HomePage(viewModel) // 暂时屏蔽下面的内容，直接显示空白主页
    /*LazyColumn(state = lazyListState) {
      item { HotWebSiteView(viewModel) }
      item { HotSearchView(viewModel) }
    }*/
  }
}

@Composable
fun HomePage(viewModel: BrowserViewModel? = null) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.align(Alignment.Center)
    ) {
      Spacer(modifier = Modifier.height(16.dp))
      val gradient = listOf(
        Color(0xFF71D78E), Color(0xFF548FE3)
      )
      Text(
        text = "Dweb-Browser",
        style = TextStyle(
          brush = Brush.linearGradient(gradient), fontSize = 36.sp
        ),
        maxLines = 1,
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IconView(
  model: Any?, text: String, onLongClick: (() -> Unit)? = null, onClick: () -> Unit
) {
  Column(modifier = Modifier.size(66.dp, 100.dp)) {
    AsyncImage(
      model = model,
      contentDescription = text,
      modifier = Modifier
        .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp))
        .padding(1.dp)
        .size(64.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.background)
        .combinedClickable(
          onClick = { onClick() },
          onLongClick = { onLongClick?.let { it() } }
        )
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = text,
      modifier = Modifier.align(Alignment.CenterHorizontally),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      fontSize = 12.sp
    )
  }
}