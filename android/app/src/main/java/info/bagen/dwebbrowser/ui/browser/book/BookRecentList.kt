package info.bagen.dwebbrowser.ui.browser.book

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.database.WebSiteInfo
import info.bagen.dwebbrowser.database.WebSiteType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInNew

val DefaultEmptyBookListContent = @Composable { modifier: Modifier ->
  Text(
    text = "暂无数据", modifier = modifier
  )
}

@Composable
fun BookRecentList(
  @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
  emptyBookListContent: (@Composable (modifier: Modifier) -> Unit) = DefaultEmptyBookListContent,
  showCount: Int = 5,
  onOpenSetting: (WebSiteInfo) -> Unit = {},
  onSearch: (String) -> Unit = {},
  onClickShowMore: () -> Unit = {},
) {
  val viewModel = LocalBookViewModel.current;
  val bookList = LocalBookViewModel.current.bookList;
  if (bookList.isEmpty()) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 100.dp)
    ) {
      emptyBookListContent(
        Modifier.align(Alignment.Center)
      )
    }
    return
  }

  val showList = bookList.takeLast(showCount).reversed()

  Column(
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "最近添加",
        modifier = Modifier.padding(
          start = (MaterialTheme.typography.titleMedium.fontSize.value / 2).dp
        ),
        style = MaterialTheme.typography.titleMedium
      )
      /// 根据内容，显示“更多的按钮”
      if (bookList.size > showList.size) {
        val textStyle =
          MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary);
        Row(
          modifier = Modifier
            .padding(end = (textStyle.fontSize.value / 2).dp)
            .clickableWithNoEffect(onClick = onClickShowMore),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "查看更多",
            style = textStyle,
          )
          Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = "More",
            modifier = Modifier
              .padding(vertical = (textStyle.fontSize.value / 4).dp)
              .size(textStyle.fontSize.value.dp),
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
    /// 列表
    Column(
      Modifier
        .verticalScroll(rememberScrollState())
        .clip(shape = MaterialTheme.shapes.large)
    ) {

      showList.forEachIndexed { index, webSiteInfo ->
        key(webSiteInfo.id) {
          if (index > 0) {
            //Divider(modifier = Modifier.padding(start = 52.dp))
            Spacer(
              modifier = Modifier.height(height = 2.dp)
            )
          }
          RowItemBook(webSiteInfo, onClick = { onSearch(it.url) })
        }
      }
    }
  }
}

@SuppressLint("RememberReturnType")
@Composable
fun Modifier.clickableWithNoEffect(onClick: () -> Unit) = this.clickable(indication = null,
  onClick = onClick,
  interactionSource = remember { MutableInteractionSource() }
)

@Composable
private fun _PreviewBrowserListOfBook(size: Int) {
  val viewModel = remember {
    BookViewModel()
  }
  for (i in 1..size) {
    viewModel.bookList.add(
      WebSiteInfo(
        id = i.toLong(),
        title = "书签 Title $i",
        url = "http://baidu.com/$i",
        type = WebSiteType.Book,
        icon = if (i % 2 != 0) null else ImageBitmap.imageResource(R.drawable.ic_launcher)
      )
    )
  }

  CompositionLocalProvider(
    LocalBookViewModel provides viewModel
  ) {
    BookRecentList()
  }
}

@Preview(showBackground = true, backgroundColor = 0x989a82)
@Composable
fun PreviewLongBrowserListOfBook() {
  _PreviewBrowserListOfBook(10)
}

@Preview(showBackground = true, backgroundColor = 0x989a82)
@Composable
fun PreviewShortBrowserListOfBook() {
  _PreviewBrowserListOfBook(3)
}

@Preview(showBackground = true, backgroundColor = 0x989a82)
@Composable
fun PreviewEmptyBrowserListOfBook() {
  _PreviewBrowserListOfBook(0)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListSwipeItem(
  webSiteInfo: WebSiteInfo,
  onRemove: (WebSiteInfo) -> Unit,
  listItemView: @Composable RowScope.() -> Unit
) {
  val dismissState = rememberDismissState(confirmValueChange = { false })
  LaunchedEffect(dismissState.currentValue) {
    if (dismissState.currentValue != DismissValue.Default) {
//      onRemove(webSiteInfo)
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
fun RowItemBook(
  webSiteInfo: WebSiteInfo,
  onClick: (WebSiteInfo) -> Unit,
) {
  ListItem(
    modifier = Modifier.clickable(onClick = { onClick(webSiteInfo) }),
    leadingContent = {
      val modifier = Modifier
//        .padding(horizontal = 12.dp)
        .size(40.dp)
      if (webSiteInfo.icon == null) {
        Icon(
          imageVector = ImageVector.vectorResource(R.drawable.ic_main_book),
          contentDescription = "Favorite",
          modifier = modifier,
          tint = MaterialTheme.colorScheme.onSurface
        )
      } else {
        Image(
          bitmap = webSiteInfo.icon,
          contentDescription = "Favorite",
          modifier = modifier
        )
      }
    },
    headlineContent = {
      Text(text = webSiteInfo.title)
    },
//    supportingContent = {
//      Text(text = webSiteInfo.url)
//    },
    trailingContent = {
      Icon(
        imageVector = Icons.Filled.OpenInNew,
        contentDescription = "Open",
      )
    },
  )
}
