package org.dweb_browser.browser.web.ui

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.data.page.BrowserPage
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.helper.compose.rememberScreenSize
import org.dweb_browser.helper.platform.theme.DimenBottomBarHeight
import org.dweb_browser.sys.window.render.LocalWindowController


/**
 * 显示多视图窗口
 */
@Composable
internal fun BrowserPreviewPanel(viewModel: BrowserViewModel) {
  if (!viewModel.showPreview) {
    return
  }
  val scope = rememberCoroutineScope()
  LocalWindowController.current.GoBackHandler {
    scope.launch {
      viewModel.updatePreviewState(false)
    }
  }
  // 高斯模糊做背景
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    //if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
    viewModel.focusPage?.thumbnail?.let { bitmap ->
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
          PreviewCellItem(viewModel, viewModel.getBrowserViewOrNull(0)!!, true)
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
          PreviewCellItem(viewModel, viewModel.getBrowserViewOrNull(it)!!, index = it)
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
          .clickable { scope.launch { viewModel.addNewPage() } },
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
          .clickable { scope.launch { viewModel.updatePreviewState(false) } },
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
fun PreviewCellItem(
  viewModel: BrowserViewModel,
  page: BrowserPage,
  onlyOne: Boolean = false,
  index: Int = 0
) {
  val screenSize = rememberScreenSize()
  val scope = rememberCoroutineScope()
  val sizeTriple = if (onlyOne) {
    val with = screenSize.screenWidth.dp - 120.dp
    Triple(with, with * 9 / 6 - 60.dp, with * 9 / 6)
  } else {
    val with = (screenSize.screenWidth.dp - 60.dp) / 2
    Triple(with, with * 9 / 6 - 40.dp, with * 9 / 6)
  }
  Box(modifier = Modifier.size(width = sizeTriple.first, height = sizeTriple.third)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Image(
        painter = page.thumbnail?.let {
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
          .clickable { scope.launch { viewModel.updatePreviewState(false, index) } }
          .align(Alignment.CenterHorizontally),
        contentScale = ContentScale.FillWidth, //ContentScale.FillBounds,
        alignment = Alignment.TopStart
      )
      val homePageTitle = BrowserI18nResource.browser_multi_startup()
      val homePageIcon = BrowserDrawResource.BrowserStar.painter()
      val homePageIconColorFilter = BrowserDrawResource.BrowserStar.getContentColorFilter()
      var contentTitle by remember { mutableStateOf(homePageTitle) }
      var contentIcon by remember { mutableStateOf<Painter?>(homePageIcon) }
      var contentIconColorFilter by remember { mutableStateOf(homePageIconColorFilter) }

      LaunchedEffect(page) {
        contentTitle = page.title
        // TODO 这里取网页图标的时候，应该根据当前颜色偏好来获取
        contentIcon = page.icon
        // TODO 这里需要有方案判断网页图标是否单色
        contentIconColorFilter = page.iconColorFilter
      }
      Row(
        modifier = Modifier
          .width(sizeTriple.first)
          .align(Alignment.CenterHorizontally)
          .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        contentIcon?.let { iconPainter ->
          Image(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            colorFilter = contentIconColorFilter
          )
          Spacer(modifier = Modifier.width(2.dp))
        }
        Text(text = contentTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
      }
    }

    if (!onlyOne) {
      Image(
        imageVector = Icons.Default.Close, //ImageVector.vectorResource(R.drawable.ic_circle_close),
        contentDescription = "Close",
        modifier = Modifier
          .clickable { scope.launch { viewModel.closePage(page) } }
          .padding(8.dp)
          .clip(CircleShape)
          .align(Alignment.TopEnd)
          .size(20.dp)
      )
    }
  }
}