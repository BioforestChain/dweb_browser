package info.bagen.dwebbrowser.ui.browser.book

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.database.WebSiteInfo
import info.bagen.dwebbrowser.database.WebSiteType

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookPage() {
  val bookView = LocalBookViewModel.current;
  FpsMonitor(
    Modifier
      .zIndex(2f)
      .fillMaxWidth()
  );
  Scaffold(
    topBar = {
      TopAppBar(
//        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
        title = { Text("书签") },
        navigationIcon = {
          IconButton(onClick = { /* doSomething() */ }) {
            Icon(
              imageVector = Icons.Filled.ArrowBackIosNew,
              contentDescription = "Localized description"
            )
          }
        },
        actions = {
          IconButton(onClick = { /* doSomething() */ }) {
            Icon(
              imageVector = Icons.Filled.Favorite,
              contentDescription = "Localized description"
            )
          }
        }
      )
    },
    content = {
      LazyColumn {
        itemsIndexed(bookView.bookList, key = { _, item -> item.id }) { index, webSiteInfo ->
          if (index > 0) {
            //Divider(modifier = Modifier.padding(start = 52.dp))
            Spacer(
              modifier = Modifier.height(height = 2.dp)
            )
          }
          RowItemBook(webSiteInfo, onClick = { })
        }
      }
    }
  )
}

@Preview
@Composable
fun PreviewBookPage() {
  val viewModel = remember {
    BookViewModel()
  }
  for (i in 1..1000) {
    viewModel.bookList.add(
      WebSiteInfo(
        id = i.toLong(),
        title = "书签 Title $i",
        url = "http://baidu.com/$i",
        type = WebSiteType.Book,
        icon = if (i % 2 != 0) null else ImageBitmap.imageResource(R.drawable.ic_launcher)
      )
    )
    viewModel.stopObserve = true;
  }

  CompositionLocalProvider(
    LocalBookViewModel provides viewModel
  ) {
    BookPage()
  }
}

@Composable
fun FpsMonitor(modifier: Modifier) {
  var fpsCount by remember { mutableStateOf(0) }
  var fps by remember { mutableStateOf(0) }
  var lastUpdate by remember { mutableStateOf(0L) }
  Text(
    text = "Fps: $fps", modifier = modifier
      .size(60.dp), color = Color.Red, style = MaterialTheme.typography.bodyLarge
  )

  LaunchedEffect(Unit) {
    while (true) {
      withFrameMillis { ms ->
        fpsCount++
        if (fpsCount == 5) {
          fps = (5000 / (ms - lastUpdate)).toInt()
          lastUpdate = ms
          fpsCount = 0
        }
      }
    }
  }
}