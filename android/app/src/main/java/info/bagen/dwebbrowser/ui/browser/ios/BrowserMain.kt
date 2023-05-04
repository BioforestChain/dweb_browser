package info.bagen.dwebbrowser.ui.browser.ios

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.microService.InternalBranch
import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import info.bagen.dwebbrowser.ui.entity.BrowserMainView
import info.bagen.dwebbrowser.ui.view.Captureable
import kotlinx.coroutines.delay

@Composable
internal fun BrowserMainView(viewModel: BrowserViewModel, browserMainView: BrowserMainView) {
  val lazyListState = rememberLazyListState()
  LaunchedEffect(lazyListState) {
    delay(100)
    snapshotFlow { lazyListState.isScrollInProgress }.collect { scroll ->
      if (!scroll) {
        delay(200); browserMainView.controller.capture()
      }
    }
  }

  Captureable(
    controller = browserMainView.controller,
    onCaptured = { imageBitmap, _ ->
      imageBitmap?.let { bitmap ->
        viewModel.uiState.currentBrowserBaseView.value.bitmap = bitmap
      }
    }) {
    HomePage() // 暂时屏蔽下面的内容，直接显示空白主页
    /*LazyColumn(state = lazyListState) {
      item { HotWebSiteView(viewModel) }
      item { InstalledApp(viewModel) }
      item { HotSearchView(viewModel) }
    }*/
  }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun HomePage(viewModel: BrowserViewModel? = null) {
  val localConfiguration = LocalConfiguration.current.screenWidthDp.dp
  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.align(Alignment.Center)
    ) {
      AsyncImage(
        model = R.drawable.ic_launcher,
        contentDescription = "home",
        modifier = Modifier.size(localConfiguration / 3)
      )
      Spacer(modifier = Modifier.height(16.dp))
      val gradient = listOf(
        Color(0xFF71D78E), Color(0xFF548FE3)
      )
      Text(
        text = stringResource(id = R.string.app_name),
        style = TextStyle(
          brush = Brush.linearGradient(gradient), fontSize = 36.sp
        ),
        maxLines = 1,
      )
    }
    viewModel?.let { if (InternalBranch) InstalledApp(it) }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IconView(
  model: Any?, text: String, onLongClick: (() -> Unit)? = null, onClick: () -> Unit
) {
  Column(modifier = Modifier.size(66.dp, 100.dp)) {
    AsyncImage(
      model = model,
      contentDescription = text,
      modifier = Modifier
        .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp))
        .padding(1.dp)
        .size(64.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.background)
        .combinedClickable(
          onClick = { onClick() },
          onLongClick = { onLongClick?.let { it() } }
        )
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = text,
      modifier = Modifier.align(Alignment.CenterHorizontally),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      fontSize = 12.sp
    )
  }
}

@Composable
private fun InstalledApp(viewModel: BrowserViewModel) {
  if (viewModel.uiState.myInstallApp.isEmpty()) return // 如果没有内容就不显示该项
  val screenWidth = LocalConfiguration.current.screenWidthDp.dp
  Column(modifier = Modifier.fillMaxWidth()) {
    /*TitleText(id = R.string.browser_main_my_app)
    Spacer(modifier = Modifier.height(10.dp))*/

    LazyVerticalGrid(
      columns = GridCells.Fixed(4),
      contentPadding = PaddingValues(20.dp),
      horizontalArrangement = Arrangement.spacedBy((screenWidth - 66.dp * 4 - 40.dp) / 3),
      verticalArrangement = Arrangement.spacedBy(0.dp),
      //modifier = Modifier.heightIn(max = 200.dp)
    ) {
      viewModel.uiState.myInstallApp.forEach { (_, value) ->
        item { DropdownMenuForInstallApp(viewModel, value.metadata) }
      }
    }
  }
}

@Composable
private fun DropdownMenuForInstallApp(viewModel: BrowserViewModel, jmmMetadata: JmmMetadata) {
  var expand by remember { mutableStateOf(false) }
  Box {
    IconView(
      model = jmmMetadata.icon,
      text = jmmMetadata.title,
      onLongClick = { expand = true },
      onClick = { viewModel.handleIntent(BrowserIntent.OpenDwebBrowser(jmmMetadata.id)) }
    )
    androidx.compose.material3.DropdownMenu(
      expanded = expand,
      onDismissRequest = { expand = false }
    ) {
      DropdownMenuItem(
        text = { Text(text = "删除") },
        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete")},
        onClick = {
          viewModel.handleIntent(BrowserIntent.UninstallJmmMetadata(jmmMetadata))
        }
      )
    }
  }
}