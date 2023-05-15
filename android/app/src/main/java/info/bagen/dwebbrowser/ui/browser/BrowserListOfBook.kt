package info.bagen.dwebbrowser.ui.browser

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.database.WebSiteDatabase
import info.bagen.dwebbrowser.database.WebSiteInfo
import info.bagen.dwebbrowser.database.WebSiteType
import info.bagen.dwebbrowser.microService.helper.ioAsyncExceptionHandler
import info.bagen.dwebbrowser.microService.helper.mainAsyncExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BrowserListOfBook(
  viewModel: BookViewModel = BookViewModel(),
  modifier: Modifier = Modifier,
  noFoundTip: (@Composable () -> Unit)? = null,
  onOpenSetting: (() -> Unit)? = null,
  onSearch: (String) -> Unit
) {
  if (viewModel.bookList.isNotEmpty()) {
    BookListContent(viewModel, modifier, onOpenSetting) { onSearch(it) }
    return
  }

  noFoundTip?.let { it() }
    ?: Box(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "暂无数据",
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 100.dp)
      )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun BookListContent(
  viewModel: BookViewModel = BookViewModel(),
  modifier: Modifier = Modifier,
  onOpenSetting: (() -> Unit)? = null,
  onSearch: (String) -> Unit
) {
  var count by remember { mutableStateOf(0) } // 初始值为 0
  AnimatedContent(
    targetState = count,
    transitionSpec = {
      if (targetState > initialState) {
        // 数字变大时，进入的界面从右向左变深划入，退出的界面从右向左变浅划出
        slideInHorizontally { fullWidth -> fullWidth } + fadeIn() with slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut()
      } else {
        // 数字变小时，进入的数字从左向右变深划入，退出的数字从左向右变浅划出
        slideInHorizontally { fullWidth -> -fullWidth } + fadeIn() with slideOutHorizontally { fullWidth -> fullWidth } + fadeOut()
      }
    }, label = ""
  ) { targetCount ->
    if (targetCount == 0) {
      LazyColumnView(viewModel, modifier, onSearch = { onSearch(it) }) {
        viewModel.currentWebSiteInfo = it
        count = 1
        onOpenSetting?.let { open -> open() }
      }
    } else {
      BookManagerView(viewModel) { count = 0 }
    }
  }
}

@Composable
private fun BookManagerView(viewModel: BookViewModel, onBack: () -> Unit) {
  val focusRequester = FocusRequester()
  val scope = rememberCoroutineScope()
  LaunchedEffect(focusRequester) {
    delay(500)
    focusRequester.requestFocus()
  }
  val webSiteInfo = viewModel.currentWebSiteInfo
    ?: WebSiteInfo(title = "无", url = "无", type = WebSiteType.Book)
  val title = remember { mutableStateOf(webSiteInfo.title) }
  val url = remember { mutableStateOf(webSiteInfo.url) }

  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = ImageVector.vectorResource(id = R.drawable.ic_more),
        contentDescription = "Backup",
        modifier = Modifier
          .graphicsLayer(rotationZ = 90f)
          .clickable { onBack() }
          .size(48.dp)
          .padding(12.dp)
      )
      Text(text = "编辑书签", modifier = Modifier.weight(1f))
      Text(
        text = "存储", modifier = Modifier
          .padding(horizontal = 12.dp)
          .clickable {
            scope.launch(ioAsyncExceptionHandler) {
              WebSiteDatabase.INSTANCE
                .websiteDao()
                .update(
                  WebSiteInfo(
                    webSiteInfo.id,
                    title.value,
                    url.value,
                    webSiteInfo.type,
                    webSiteInfo.timeMillis,
                    webSiteInfo.icon
                  )
                )
              onBack()
            }
          },
        color = MaterialTheme.colorScheme.primary
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
        .height(100.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.background)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        webSiteInfo.icon?.let { bitmap ->
          Icon(
            bitmap = bitmap,
            contentDescription = "icon",
            modifier = Modifier
              .padding(12.dp)
              .size(48.dp)
          )
        } ?: Icon(
          ImageVector.vectorResource(R.drawable.ic_main_book),
          contentDescription = "icon",
          modifier = Modifier
            .padding(12.dp)
            .size(48.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
          CustomTextField(
            value = title.value,
            onValueChange = { title.value = it },
            modifier = Modifier
              .height(48.dp)
              .focusRequester(focusRequester),
            trailingIcon = {
              Icon(
                Icons.Filled.Close,
                contentDescription = "Close",
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.outlineVariant)
                  .padding(2.dp)
              )
            }
          )
          Divider()
          CustomTextField(
            value = url.value,
            onValueChange = { url.value = it },
            modifier = Modifier.height(48.dp),
            trailingIcon = {
              Icon(
                Icons.Filled.Close,
                contentDescription = "Close",
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.outlineVariant)
                  .padding(2.dp)
              )
            }
          )
        }
      }
    }

    Button(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 50.dp),
      onClick = {
      scope.launch(ioAsyncExceptionHandler) {
        WebSiteDatabase.INSTANCE.websiteDao().delete(webSiteInfo)
        onBack()
      }
    }) {
      Text(text = "删除")
    }
  }
}

@Composable
private fun LazyColumnView(
  viewModel: BookViewModel,
  modifier: Modifier = Modifier,
  onSearch: (String) -> Unit,
  openSetting: (WebSiteInfo) -> Unit
) {
  LazyColumn(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
    items(viewModel.bookList) { webSiteInfo ->
      ListSwipeItem(
        webSiteInfo = webSiteInfo,
        onRemove = {
          viewModel.bookList.remove(it)
          WebSiteDatabase.INSTANCE.websiteDao().delete(it)
        }) {
        ListItem(
          headlineContent = {
            Text(text = webSiteInfo.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
          },
          modifier = Modifier.clickable {
            onSearch(webSiteInfo.url)
          },
          leadingContent = {
            webSiteInfo.icon?.let { icon ->
              Icon(bitmap = icon, contentDescription = webSiteInfo.title, Modifier.size(18.dp))
            } ?: Icon(
              ImageVector.vectorResource(R.drawable.ic_main_book),
              webSiteInfo.title,
              Modifier.size(22.dp)
            )
          },
          trailingContent = {
            Icon(
              ImageVector.vectorResource(id = R.drawable.ic_more),
              contentDescription = "Expand",
              modifier = Modifier
                .clickable { openSetting(webSiteInfo) }
                .size(22.dp)
                .graphicsLayer(rotationZ = -90f)
            )
          }
        )
      }
      Divider()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListSwipeItem(
  webSiteInfo: WebSiteInfo, onRemove: (WebSiteInfo) -> Unit, listItemView: @Composable () -> Unit
) {
  val dismissState = DismissState(DismissValue.Default, { true }, SwipeToDismissDefaults.FixedPositionalThreshold)
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
          .background(MaterialTheme.colorScheme.outlineVariant))
    },
    dismissContent = { // ”前景“ 显示的内容
        listItemView()
    },
    directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart)
  )
}

class BookViewModel : ViewModel() {
  val bookList: MutableList<WebSiteInfo> = mutableStateListOf()
  var currentWebSiteInfo: WebSiteInfo? = null

  init {
    viewModelScope.launch(mainAsyncExceptionHandler) {
      WebSiteDatabase.INSTANCE.websiteDao().loadAllByTypeObserve(WebSiteType.Book)
        .observeForever {
          bookList.clear()
          it.forEach { webSiteInfo ->
            bookList.add(webSiteInfo)
          }
        }
    }
  }
}