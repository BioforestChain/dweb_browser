package org.dweb_browser.browser.web.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.data.page.BrowserBookmarkPage
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.helper.compose.NoDataRender
import org.dweb_browser.sys.toast.PositionType
import org.dweb_browser.sys.window.render.LocalWindowController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserBookmarkPageRender(
  page: BrowserBookmarkPage,
  modifier: Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
  val isInEditMode = page.isInEditMode
  LocalWindowController.current.GoBackHandler(isInEditMode) {
    page.isInEditMode = false
  }
  val viewModal = LocalBrowserViewModel.current
  val uiScope = rememberCoroutineScope()
  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = WindowInsets(0),
    topBar = {
      CenterAlignedTopAppBar(
        windowInsets = WindowInsets(0, 0, 0, 0), // 顶部
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
          Text(
            BrowserI18nResource.browser_bookmark_page_title(), overflow = TextOverflow.Ellipsis
          )
        },
        /// 左上角导航按钮
        navigationIcon = {
          if (isInEditMode) {
            IconButton(onClick = { page.isInEditMode = false }) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                contentDescription = "Back to list"
              )
            }
          }
        },
        /// 右上角功能按钮
        actions = {
          if (isInEditMode) {
            IconButton(enabled = page.selectedBookmarks.isNotEmpty(), onClick = {
              uiScope.launch {
                viewModal.removeBookmarkUI(items = page.selectedBookmarks.toTypedArray())
                page.isInEditMode = false
              }
            }) {
              Icon(
                imageVector = Icons.Filled.Delete, contentDescription = "Delete Selects"
              )
            }
          } else {
            IconButton(onClick = {
              page.isInEditMode = true
              page.selectedBookmarks.clear()
              viewModal.showToastMessage(
                BrowserI18nResource.browser_bookmark_edit_tip.text,
                position = PositionType.TOP
              )
            }) {
              Icon(
                imageVector = Icons.Filled.Edit, contentDescription = "Go to Edit"
              )
            }
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    BrowserBookmarkListPage(page, Modifier.padding(innerPadding))
  }
}

@Composable
fun BrowserBookmarkListPage(page: BrowserBookmarkPage, modifier: Modifier) {
  val isInEditMode = page.isInEditMode
  val viewModel = LocalBrowserViewModel.current
  val bookmarks = viewModel.getBookmarks()
  val uiScope = rememberCoroutineScope()
  if (bookmarks.isEmpty()) {
    NoDataRender(BrowserI18nResource.browser_empty_list(), modifier = modifier)
  } else {
    BrowserBookmarkItemEditDialog(page)
    LazyColumn(modifier = modifier.fillMaxSize()) {
      items(bookmarks) { bookmark ->
        val openInNewPage = remember(viewModel, bookmark) {
          {
            uiScope.launch { viewModel.tryOpenUrlUI(bookmark.url) }
            Unit
          }
        }

        ListItem(
          // 图标
          leadingContent = {

            bookmark.iconImage?.also { imageBitmap ->
              Image(
                bitmap = imageBitmap,
                contentDescription = bookmark.title,
                modifier = Modifier.padding(horizontal = 12.dp).size(28.dp)
              )
            } ?: Icon(
              imageVector = Icons.Default.Bookmark,// ImageVector.vectorResource(R.drawable.ic_main_book),
              contentDescription = bookmark.title,
              modifier = Modifier.padding(horizontal = 12.dp).size(28.dp),
              tint = MaterialTheme.colorScheme.onSurface
            )
          },
          // 绑定点击行为，弹出编辑对话框、或者跳转
          modifier = Modifier.clickable {
            if (isInEditMode) {
              page.editingBookmark = bookmark
            } else {
              openInNewPage()
            }
          }, headlineContent = {
            Text(text = bookmark.title, overflow = TextOverflow.Ellipsis)
          }, supportingContent = {
            Text(text = bookmark.url, overflow = TextOverflow.Ellipsis)
          }, trailingContent = {
            if (isInEditMode) {
              Checkbox(checked = page.selectedBookmarks.contains(bookmark), {
                when (it) {
                  true -> page.selectedBookmarks.add(bookmark)
                  else -> page.selectedBookmarks.remove(bookmark)
                }
              })
            } else {
              IconButton(openInNewPage) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, // ImageVector.vectorResource(R.drawable.ic_more),
                  contentDescription = "Open In New Page",
                  tint = MaterialTheme.colorScheme.outlineVariant
                )
              }
            }
          })
      }
    }
  }
}


@Composable
fun BrowserBookmarkItemEditDialog(page: BrowserBookmarkPage, modifier: Modifier = Modifier) {
  val editingBookmark = page.editingBookmark ?: return
  var editedBookmark by remember(editingBookmark) { mutableStateOf(editingBookmark.copy()) }
  val viewModel = LocalBrowserViewModel.current
  val uiScope = rememberCoroutineScope()

  AlertDialog(icon = {
    Icon(
      Icons.Default.Edit,
      contentDescription = BrowserI18nResource.browser_bookmark_edit_dialog_title()
    )
  }, title = {
    Text(BrowserI18nResource.browser_bookmark_edit_dialog_title())
  }, text = {
    Column {
      TextField(value = editedBookmark.title,
        onValueChange = { editedBookmark = editedBookmark.copy(title = it) },
        label = { Text(BrowserI18nResource.browser_bookmark_title()) })
      TextField(value = editedBookmark.url,
        onValueChange = { editedBookmark = editedBookmark.copy(url = it) },
        label = { Text(BrowserI18nResource.browser_bookmark_url()) })
    }
  }, onDismissRequest = {
    page.editingBookmark = null
  }, confirmButton = {
    TextButton(enabled = editingBookmark != editedBookmark, onClick = {
      uiScope.launch {
        viewModel.updateBookmarkUI(editingBookmark, editedBookmark)
      }
    }) {
      Text(BrowserI18nResource.button_name_confirm())
    }
  }, dismissButton = {
    TextButton(onClick = {
      page.editingBookmark = null
    }) {
      Text(BrowserI18nResource.button_name_cancel())
    }
  })
}

