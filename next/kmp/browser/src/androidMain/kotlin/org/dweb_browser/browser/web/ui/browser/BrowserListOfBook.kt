package org.dweb_browser.browser.web.ui.browser

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.R
import org.dweb_browser.browser.web.ui.browser.model.BrowserViewModel
import org.dweb_browser.browser.web.ui.browser.model.WebSiteInfo
import org.dweb_browser.helper.*

@Composable
fun BrowserListOfBook(
  viewModel: BrowserViewModel,
  @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
  noFoundTip: (@Composable () -> Unit)? = null,
  onOpenSetting: (WebSiteInfo) -> Unit,
  onSearch: (String) -> Unit
) {
  val bookLinks = viewModel.getBookLinks()
  val scope = rememberCoroutineScope()
  if (bookLinks.isNotEmpty()) {
    BookListContent(
      bookList = bookLinks,
      modifier = modifier,
      onDelete = { scope.launch { viewModel.changeBookLink(del = it) } },
      onOpenSetting = onOpenSetting
    ) { onSearch(it) }
    return
  }

  noFoundTip?.let { it() }
    ?: Box(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = stringResource(id = R.string.browser_empty_list),
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 100.dp)
      )
    }
}

@Composable
private fun BookListContent(
  bookList: MutableList<WebSiteInfo>,
  @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
  onDelete: (WebSiteInfo) -> Unit,
  onOpenSetting: (WebSiteInfo) -> Unit,
  onSearch: (String) -> Unit
) {
  LazyColumn(
    modifier = modifier
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
  ) {
    itemsIndexed(bookList) { index, webSiteInfo ->
      if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surface)
      ListSwipeItem(
        webSiteInfo = webSiteInfo,
        onRemove = { onDelete(it) }
      ) {
        val shape = when (index) {
          0 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
          bookList.size - 1 -> RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
          else -> RoundedCornerShape(0.dp)
        }
        RowItemBook(webSiteInfo, shape, { onSearch(it.url) }) { onOpenSetting(it) }
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
  val dismissState = // rememberDismissState() // 不能用这个，不然会导致移除后remember仍然存在，显示错乱问题
    DismissState(
      initialValue = DismissValue.Default,
      density = LocalDensity.current,
      confirmValueChange = { true },
      positionalThreshold = SwipeToDismissDefaults.fixedPositionalThreshold
    )
  LaunchedEffect(dismissState) {
    snapshotFlow { dismissState.currentValue }.collect {
      if (it != DismissValue.Default) {
        onRemove(webSiteInfo)
      }
    }
  }
  Row { listItemView() }

  /*SwipeToDismiss(
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
  )*/
}

@Composable
private fun RowItemBook(
  webSiteInfo: WebSiteInfo,
  shape: RoundedCornerShape,
  onClick: (WebSiteInfo) -> Unit,
  onOpenSetting: (WebSiteInfo) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(50.dp)
      .clip(shape)
      .background(MaterialTheme.colorScheme.surface)
      .clickable { onClick(webSiteInfo) },
    verticalAlignment = Alignment.CenterVertically
  ) {
    webSiteInfo.icon?.let { imageBitmap ->
      Image(
        bitmap = imageBitmap,
        contentDescription = "Icon",
        modifier = Modifier
          .padding(horizontal = 12.dp)
          .size(28.dp)
      )
    } ?: run {
      Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_main_book),
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
      imageVector = ImageVector.vectorResource(R.drawable.ic_more),
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