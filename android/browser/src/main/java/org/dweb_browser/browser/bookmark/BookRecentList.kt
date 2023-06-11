package org.dweb_browser.browser.bookmark

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.graphics.Color

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
  onOpenSetting: (Bookmark) -> Unit = {},
  onSearch: (String) -> Unit = {},
  onClickShowMore: () -> Unit = {},
) {
  val viewModel = LocalBookmarkView.current;
  val bookList = LocalBookmarkView.current.bookList;
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
            Divider(color = Color.Transparent)
          }
          RowItemBook(
            data = webSiteInfo,
            modifier = Modifier.clickable(onClick = { onSearch(webSiteInfo.url) })
          )
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
    BookmarkView()
  }
  for (i in 1..size) {
    viewModel.bookList.add(
      Bookmark(
        id = i,
        title = "书签 Title $i",
        url = "http://baidu.com/$i",
        icon = if (i % 2 != 0) null else ImageBitmap.imageResource(androidx.core.R.drawable.ic_call_decline_low)
      )
    )
  }

  CompositionLocalProvider(
    LocalBookmarkView provides viewModel
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

