package org.dweb_browser.browserUI.bookmark

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.dweb_browser.browserUI.R
import org.dweb_browser.browserUI.bookmark.BookPage.BookPageModel
import org.dweb_browser.browserUI.bookmark.BookPage.BookmarkEditModeDropdownMenu
import org.dweb_browser.browserUI.bookmark.BookPage.BookmarkEditModeEditDialog
import org.dweb_browser.browserUI.bookmark.BookPage.BookmarkEditModel
import org.dweb_browser.browserUI.bookmark.BookPage.LocalBookPageModel
import org.dweb_browser.browserUI.bookmark.BookPage.LocalBookmarkEditModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookPage() {
  val bookView = LocalBookmarkView.current;
  var editMode by LocalBookPageModel.current.editMode
  FpsMonitor(
    Modifier
      .zIndex(2f)
      .fillMaxWidth()
      .padding(horizontal = 32.dp)
  );
  Scaffold(topBar = {
    TopAppBar(
//        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
      title = { Text("书签") }, navigationIcon = {
        IconButton(onClick = { /* doSomething() */ }) {
          Icon(
            imageVector = Icons.Filled.ArrowBackIosNew, contentDescription = "返回上一页"
          )
        }
      }, actions = {
        IconButton(onClick = { /*TODO*/ }) {
          Icon(
            imageVector = Icons.Filled.Search, contentDescription = "搜索"
          )
        }
        IconButton(onClick = { editMode = !editMode }) {
          if (editMode) {
            /// 返回取消编辑模式
            BackHandler {
              editMode = false
            }
            Icon(
              imageVector = Icons.Filled.Check, contentDescription = "完成"
            )
          } else {
            Icon(
              imageVector = Icons.Filled.ModeEdit, contentDescription = "开始编辑"
            )
          }
        }
      })
  }, content = {
    LazyColumn(
      modifier = Modifier.padding(it),
    ) {
      itemsIndexed(bookView.bookList) { index, webSiteInfo ->

        RowItemBook(webSiteInfo, trailingContent = if (editMode) {
          {
            val bookmarkEditModel = remember {
              BookmarkEditModel(webSiteInfo)
            }
            Icon(Icons.Filled.MoreVert,
              contentDescription = "More Edit Options",
              modifier = Modifier
                .size(24.dp)
                .padding(4.dp)
                .clickable { bookmarkEditModel.showMoreOptions.value = true })

            CompositionLocalProvider(LocalBookmarkEditModel provides bookmarkEditModel) {
              BookmarkEditModeDropdownMenu()
              BookmarkEditModeEditDialog()
            }

          }
        } else null)

      }
    }
  })
}


@Composable
fun _previewBookPage(pageModel: BookPageModel) {
  val viewModel = remember {
    BookmarkView()
  }
  for (i in 1..1000) {
    viewModel.bookList.add(
      Bookmark(
        id = i,
        title = "书签 Title $i",
        url = "http://baidu.com/$i",
        icon = if (i % 2 != 0) null else ImageBitmap.imageResource(R.drawable.ic_launcher)
      )
    )
    viewModel.stopObserve = true;
  }

  CompositionLocalProvider(
    LocalBookmarkView provides viewModel,
    LocalBookPageModel provides pageModel,
  ) {
    BookPage()
  }
}

@Preview
@Composable
fun PreviewBookPage1() {
  val pageModel = remember {
    BookPageModel()
  }
  _previewBookPage(pageModel)
}

@Preview
@Composable
fun PreviewBookPage2() {
  val pageModel = remember {
    BookPageModel()
  }
  pageModel.editMode.value = true

  _previewBookPage(pageModel)
}

@Composable
fun FpsMonitor(modifier: Modifier) {
  var lastUpdate = 0L
  var fps by remember { mutableIntStateOf(0) }
  var count = 0;
  var preMs = 0L
  Text(
    text = "FPS: $fps",
    modifier = modifier.size(60.dp),
    color = Color.Red,
    style = MaterialTheme.typography.bodyLarge
  )

  LaunchedEffect(Unit) {
    while (true) {
      withFrameMillis { ms ->
        if (
        // 30 帧率更新一次
          ++count == 30
          // 帧率小于50
          || (ms - preMs) > 20
        ) {
          val newFps = (count * 1000 / (ms - lastUpdate)).toInt()
          if (fps != newFps) {
            fps = newFps
          }
          lastUpdate = ms
          count = 0;
        }
        preMs = ms
      }
    }
  }
}