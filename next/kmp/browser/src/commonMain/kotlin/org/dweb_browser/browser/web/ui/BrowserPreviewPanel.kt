package org.dweb_browser.browser.web.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.model.page.BrowserHomePage
import org.dweb_browser.browser.web.model.page.BrowserPage
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.helper.compose.IosFastOutSlowInEasing
import org.dweb_browser.helper.platform.theme.DimenBottomBarHeight
import org.dweb_browser.sys.window.render.LocalWindowController


fun <T> enterAnimationSpec() = tween<T>(250, easing = IosFastOutSlowInEasing)
fun <T> exitAnimationSpec() = tween<T>(300, easing = IosFastOutSlowInEasing)

/**
 * 显示多视图窗口
 */
@Composable
internal fun BrowserPreviewPanel(
  viewModel: BrowserViewModel, modifier: Modifier = Modifier,
) = AnimatedVisibility(
  viewModel.showPreview,
  modifier = modifier,
  enter = scaleIn(enterAnimationSpec(), 1.6f) + fadeIn(enterAnimationSpec()),
  exit = scaleOut(exitAnimationSpec(), 1.6f) + fadeOut(exitAnimationSpec())
) {
  // 使用动画替代
  //  if (!viewModel.showPreview) {
  //    return
  //  }
  val uiScope = rememberCoroutineScope()
  LocalWindowController.current.GoBackHandler {
    viewModel.showPreview = false
  }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
    val focusedPageIndex = viewModel.focusedPageIndex
    val lazyGridState = rememberLazyGridState(focusedPageIndex)
    BoxWithConstraints(
      modifier = Modifier.weight(1f),
    ) {
      val pageSize = viewModel.pageSize
      val onlyOne = pageSize <= 1
      val cellWidth = remember(onlyOne, maxWidth) {
        when (onlyOne) {
          true -> maxWidth * 0.618f
          else -> maxWidth * 0.8f / 2
        }
      }
      val cellHeight = remember(cellWidth) {
        cellWidth * 1.618f
      }
      LazyVerticalGrid(
        columns = GridCells.Fixed(if (onlyOne) 1 else 2),
        modifier = Modifier.fillMaxSize(),
        state = lazyGridState,
        contentPadding = PaddingValues(vertical = 20.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        items(pageSize) { pageIndex ->
          val page = viewModel.getPage(pageIndex)
          PagePreviewCell(
            page,
            Modifier.requiredSize(cellWidth, cellHeight + 16.dp).padding(bottom = 16.dp),
            closable = !(pageSize == 1 && page is BrowserHomePage),
            focusedPageIndex == pageIndex
          )
        }
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth().height(DimenBottomBarHeight)
        .background(MaterialTheme.colorScheme.surface), verticalAlignment = CenterVertically
    ) {
      IconButton({
        uiScope.launch {
          viewModel.addNewPageUI {
            addIndex = focusedPageIndex
            focusPage = false
          }
        }
        viewModel.showPreview = false
      }) {
        Icon(
          imageVector = Icons.Default.Add, // ImageVector.vectorResource(id = R.drawable.ic_main_add),
          contentDescription = "Add New Page",
          tint = MaterialTheme.colorScheme.primary,
        )
      }
      val content = BrowserI18nResource.browser_multi_count()
      Text(
        text = "${viewModel.pageSize} $content",
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center
      )
      TextButton({ viewModel.showPreview = false }) {
        Text(
          text = BrowserI18nResource.browser_multi_done(),
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}


@Composable
private fun PagePreviewCell(
  page: BrowserPage, modifier: Modifier, closable: Boolean, focus: Boolean
) {
  val viewModel = LocalBrowserViewModel.current
  val scope = viewModel.browserNMM.ioAsyncScope
  val uiScope = rememberCoroutineScope()

  Box(modifier) {
    if (closable) {
      IconButton(
        {
          scope.launch { viewModel.closePageUI(page) }
        },
        modifier = Modifier.align(Alignment.TopEnd).zIndex(2f),
        colors = IconButtonDefaults.iconButtonColors()
          .copy(containerColor = MaterialTheme.colorScheme.primaryContainer)
      ) {
        Icon(
          Icons.Default.Close,
          contentDescription = "Close Page",
        )
      }
    }
    Column(
      modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
      val pageTitle = page.title
      val pageIcon = page.icon
      val pageIconColorFilter = page.iconColorFilter
      val pagePreview = page.previewContent
      BoxWithConstraints(
        Modifier.weight(1f, true).fillMaxWidth()
          .shadow(elevation = if (focus) 4.dp else 1.dp, shape = RoundedCornerShape(16.dp))
          .clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable {
            uiScope.launch {
              viewModel.focusPageUI(page)
              viewModel.showPreview = false
            }
          }.align(Alignment.CenterHorizontally),
        contentAlignment = Alignment.Center,
      ) {
        if (pagePreview != null) {
          Image(
            painter = pagePreview,
            contentDescription = pageTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.Center
          )
        } else if (pageIcon != null) {
          Image(
            painter = pageIcon,
            contentDescription = pageTitle,
            colorFilter = pageIconColorFilter,
            modifier = Modifier.size(maxWidth / 3),
          )
        } else {
          Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = pageTitle,
            modifier = Modifier.size(maxWidth / 3),
            tint = LocalContentColor.current.copy(alpha = 0.5f)
          )
        }
      }
      Row(
        modifier = Modifier.wrapContentHeight().fillMaxWidth().padding(top = 4.dp)
          .align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        pageIcon?.let { iconPainter ->
          Image(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            colorFilter = pageIconColorFilter
          )
          Spacer(modifier = Modifier.width(2.dp))
        }
        Text(text = pageTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
      }
    }
  }
}