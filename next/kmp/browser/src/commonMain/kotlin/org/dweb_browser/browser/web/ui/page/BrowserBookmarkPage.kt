package org.dweb_browser.browser.web.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.data.WebSiteInfo
import org.dweb_browser.browser.web.data.page.BrowserBookmarkPage
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.ui.CustomTextField
import org.dweb_browser.helper.compose.NoDataRender
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
  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

    topBar = {
      CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
          Text(
            "Centered Top App Bar", maxLines = 1, overflow = TextOverflow.Ellipsis
          )
        },
        actions = {
          if (isInEditMode) {
            IconButton(onClick = { page.isInEditMode = false }) {
              Icon(
                imageVector = Icons.Filled.Check, contentDescription = "Back to list"
              )
            }
          } else {
            IconButton(onClick = { page.isInEditMode = true }) {
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
    BrowserBookmarkListPage(Modifier.padding(innerPadding), isInEditMode)
  }
}

@Composable
fun BrowserBookmarkListPage(modifier: Modifier, isInEditMode: Boolean) {
  val viewModel = LocalBrowserViewModel.current
  val bookmarks = viewModel.getBookmarks()
  val uiScope = rememberCoroutineScope()
  if (bookmarks.isEmpty()) {
    NoDataRender(BrowserI18nResource.browser_empty_list(), modifier = modifier)
  } else {
    LazyColumn(modifier = modifier.fillMaxSize()) {
      items(bookmarks) { bookmark ->
        val openInNewPage = remember(viewModel, bookmark) {
          {
            uiScope.launch { viewModel.tryOpenUrl(bookmark.url) }
            Unit
          }
        }
        val removeBookmark = remember(viewModel, bookmark) {
          {
            viewModel.removeBookmark(bookmark)
            Unit
          }
        }

        ListItem(leadingContent = {
          bookmark.iconImage?.also { imageBitmap ->
            Image(
              bitmap = imageBitmap,
              contentDescription = bookmark.title,
              modifier = Modifier.padding(horizontal = 12.dp).size(28.dp)
            )
          } ?: Icon(
            imageVector = Icons.Default.Book,// ImageVector.vectorResource(R.drawable.ic_main_book),
            contentDescription = bookmark.title,
            modifier = Modifier.padding(horizontal = 12.dp).size(28.dp),
            tint = MaterialTheme.colorScheme.onSurface
          )
        }, modifier = Modifier.clickable {
          if (isInEditMode) {
            // TODO 显示一个Alert对话框，从而可以进行详情编辑
          } else {
            openInNewPage()
          }
        }, headlineContent = {
          Text(text = bookmark.title, overflow = TextOverflow.Ellipsis)
        }, supportingContent = {
          Text(text = bookmark.url, overflow = TextOverflow.Ellipsis)
        }, trailingContent = {
          if (isInEditMode) {
            IconButton(removeBookmark) {
              Icon(
                imageVector = Icons.Default.Delete, // ImageVector.vectorResource(R.drawable.ic_more),
                contentDescription = "Remove This Bookmark",
              )
            }
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
private fun RowItemBook(
  webSiteInfo: WebSiteInfo, onClick: (WebSiteInfo) -> Unit, onOpenSetting: (WebSiteInfo) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth().height(72.dp).background(MaterialTheme.colorScheme.surface)
      .clickable { onClick(webSiteInfo) }, verticalAlignment = Alignment.CenterVertically
  ) {
    webSiteInfo.iconImage?.let { imageBitmap ->
      Image(
        bitmap = imageBitmap,
        contentDescription = "Icon",
        modifier = Modifier.padding(horizontal = 12.dp).size(28.dp)
      )
    } ?: run {
      Icon(
        imageVector = Icons.Default.Book,// ImageVector.vectorResource(R.drawable.ic_main_book),
        contentDescription = "Book",
        modifier = Modifier.padding(horizontal = 12.dp).size(28.dp),
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
      modifier = Modifier.clickable { onOpenSetting(webSiteInfo) }.padding(horizontal = 12.dp)
        .size(20.dp).graphicsLayer(rotationZ = -90f),
      tint = MaterialTheme.colorScheme.outlineVariant
    )
  }
}

///**
// * 书签管理界面
// */
//@Composable
//fun BrowserBookmarkEditPage(modifier: Modifier) {
//  val viewModel = LocalBrowserViewModel.current
//  val inputTitle = remember { mutableStateOf(webSiteInfo.value?.title ?: "") }
//  val inputUrl = remember { mutableStateOf(webSiteInfo.value?.url ?: "") }
//  Column(modifier = Modifier.fillMaxSize()) {
//    BrowserManagerTitle(
//      title = BrowserI18nResource.browser_options_editBook(),
//      onBack = onBack,
//      onDone = {
//        webSiteInfo.value?.apply {
//          title = inputTitle.value
//          url = inputUrl.value
//          viewModel.updateBookLink(this)
//          onBack()
//        }
//      }
//    )
//    val item = webSiteInfo.value ?: return
//    val focusRequester = remember { FocusRequester() }
//    LaunchedEffect(focusRequester) {
//      delay(100)
//      focusRequester.requestFocus()
//    }
//
//    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
//      RowItemTextField(
//        leadingBitmap = item.iconImage,
//        leadingIcon = Icons.Default.Book,
//        inputText = inputTitle,
//        focusRequester = focusRequester
//      )
//      Spacer(modifier = Modifier.height(16.dp))
//      RowItemTextField(leadingIcon = Icons.Default.Link, inputText = inputUrl)
//      Spacer(modifier = Modifier.height(16.dp))
//      Box(
//        modifier = Modifier
//          .fillMaxWidth()
//          .height(50.dp)
//          .clip(RoundedCornerShape(6.dp))
//          .background(MaterialTheme.colorScheme.surfaceVariant)
//          .clickable {
//            viewModel.removeBookLink(item)
//            onBack()
//          },
//        contentAlignment = Alignment.Center
//      ) {
//        Text(
//          text = BrowserI18nResource.browser_options_delete(),
//          color = MaterialTheme.colorScheme.error,
//          fontSize = 16.sp,
//          fontWeight = FontWeight(400)
//        )
//      }
//    }
//  }
//}

@Composable
private fun RowItemTextField(
  leadingBitmap: ImageBitmap? = null,
  leadingIcon: ImageVector,
  inputText: MutableState<String>,
  focusRequester: FocusRequester? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(6.dp))
      .background(MaterialTheme.colorScheme.surface), verticalAlignment = Alignment.CenterVertically
  ) {
    val modifier = focusRequester?.let { Modifier.focusRequester(focusRequester) } ?: Modifier

    CustomTextField(value = inputText.value,
      onValueChange = { inputText.value = it },
      modifier = modifier,
      spacerWidth = 0.dp,
      leadingIcon = {
        leadingBitmap?.let {
          Image(
            bitmap = it,
            contentDescription = "Icon",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp).size(28.dp)
          )
        } ?: run {
          Icon(
            imageVector = leadingIcon,
            contentDescription = "Icon",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp).size(28.dp),
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      })
  }
}