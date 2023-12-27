package org.dweb_browser.browser.web.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissState
import androidx.compose.material3.DismissValue
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.model.WebSiteInfo
import org.dweb_browser.browser.web.ui.model.BrowserViewModel

@Composable
fun BrowserListOfBook(
  viewModel: BrowserViewModel,
  modifier: Modifier = Modifier,
  noFoundTip: (@Composable () -> Unit)? = null,
  onOpenSetting: (WebSiteInfo) -> Unit,
  onSearch: (String) -> Unit
) {
  LazyColumn(
    modifier = modifier
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
      .clip(RoundedCornerShape(6.dp))
  ) {
    val bookLinks = viewModel.getBookLinks()
    if (bookLinks.isEmpty()) {
      item {
        noFoundTip?.let { it() }
          ?: Box(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = BrowserI18nResource.browser_empty_list(),
              modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
            )
          }
      }
      return@LazyColumn
    }

    itemsIndexed(bookLinks) { index, webSiteInfo ->
      if (index > 0) Divider(
        modifier = Modifier.width(1.dp), color = MaterialTheme.colorScheme.surface
      )
      ListSwipeItem(
        webSiteInfo = webSiteInfo,
        onRemove = { viewModel.removeBookLink(it) }
      ) {
        RowItemBook(webSiteInfo, { onSearch(it.url) }) { onOpenSetting(it) }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListSwipeItem(
  webSiteInfo: WebSiteInfo,
  onRemove: (WebSiteInfo) -> Unit,
  listItemView: @Composable RowScope.() -> Unit
) {
  // val dismissState = rememberDismissState() // 不能用这个，不然会导致移除后remember仍然存在，显示错乱问题
  val dismissState = DismissState(
    initialValue = DismissValue.Default,
    confirmValueChange = { true },
    positionalThreshold = { density -> 56.0f * density },
  )
  LaunchedEffect(dismissState) {
    snapshotFlow { dismissState.currentValue }.collect {
      if (it != DismissValue.Default) {
        onRemove(webSiteInfo)
      }
    }
  }

  SwipeToDismiss(
    state = dismissState,
    background = { // "背景 "，即原来显示的内容被划走一部分时显示什么
      Box(
        Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
      )
    },
    dismissContent = { // ”前景“ 显示的内容
      listItemView()
    },
    directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart)
  )
}

@Composable
private fun RowItemBook(
  webSiteInfo: WebSiteInfo,
  onClick: (WebSiteInfo) -> Unit,
  onOpenSetting: (WebSiteInfo) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(50.dp)
      .background(MaterialTheme.colorScheme.surface)
      .clickable { onClick(webSiteInfo) },
    verticalAlignment = Alignment.CenterVertically
  ) {
    webSiteInfo.iconImage?.let { imageBitmap ->
      Image(
        bitmap = imageBitmap,
        contentDescription = "Icon",
        modifier = Modifier
          .padding(horizontal = 12.dp)
          .size(28.dp)
      )
    } ?: run {
      Icon(
        imageVector = Icons.Default.Book,// ImageVector.vectorResource(R.drawable.ic_main_book),
        contentDescription = "Book",
        modifier = Modifier
          .padding(horizontal = 12.dp)
          .size(28.dp),
        tint = MaterialTheme.colorScheme.onSurface
      )
    }

    Text(
      text = webSiteInfo.title,
      modifier = Modifier.weight(1f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Icon(
      imageVector = Icons.Default.ExpandMore, // ImageVector.vectorResource(R.drawable.ic_more),
      contentDescription = "Manager",
      modifier = Modifier
        .clickable { onOpenSetting(webSiteInfo) }
        .padding(horizontal = 12.dp)
        .size(20.dp)
        .graphicsLayer(rotationZ = -90f),
      tint = MaterialTheme.colorScheme.outlineVariant
    )
  }
}