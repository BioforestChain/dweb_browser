package info.bagen.dwebbrowser.microService.browser.desk.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.dweb_browser.window.core.constant.WindowBottomBarTheme
import org.dweb_browser.window.core.constant.WindowMode
import org.dweb_browser.window.render.LocalWindowController
import org.dweb_browser.window.render.WindowPreviewer


@Composable
fun PreviewWindowBottomBarContent(modifier: Modifier) {
  val win = LocalWindowController.current
  var pageIndex by remember {
    mutableIntStateOf(0)
  }
  val mockPages = remember { mutableStateListOf("page 1") }
  win.state.canGoBack = pageIndex > 0
  while (mockPages.size <= pageIndex) {
    mockPages.add("page ${mockPages.size + 1}")
  }

  if (win.state.canGoForward != null) {
    win.state.canGoForward = mockPages.size > (pageIndex + 1)

  }
  win.GoBackHandler {
    pageIndex -= 1
  }
  win.GoForwardHandler {
    pageIndex += 1
  }


  Box(
    modifier
      .background(Color.DarkGray)
      .clickable {
        pageIndex += 1
      }) {
    Text(
      text = "当前页面：${mockPages[pageIndex]}", modifier = Modifier.align(Alignment.Center)
    )
  }
}

@Preview(widthDp = 400, heightDp = 120)
@Composable
fun PreviewWindowBottomNavigationBar() {
  WindowPreviewer(config = {
    state.bottomBarTheme = WindowBottomBarTheme.Navigation
    state.bottomBarContentColor = "#FF00FF"
  }) { modifier ->
    PreviewWindowBottomBarContent(modifier)
  }
}

@Preview(widthDp = 400, heightDp = 120)
@Composable
fun PreviewWindowBottomNavigationBarWithMax() {
  WindowPreviewer(config = {
    state.bottomBarTheme = WindowBottomBarTheme.Navigation
    state.mode = WindowMode.MAXIMIZE
    state.bottomBarContentColor = "#FF00FF"
    state.canGoForward = false
  }) { modifier ->
    PreviewWindowBottomBarContent(modifier)
  }
}

@Preview(widthDp = 400, heightDp = 120)
@Composable
fun PreviewWindowBottomImmersionBar() {
  WindowPreviewer(config = {
    state.bottomBarTheme = WindowBottomBarTheme.Immersion
    state.bottomBarContentColor = "#FF00FF"
  }) { modifier ->
    PreviewWindowBottomBarContent(modifier)
  }
}

@Preview(widthDp = 400, heightDp = 120)
@Composable
fun PreviewWindowBottomImmersionBarWithMax() {
  WindowPreviewer(config = {
    state.bottomBarTheme = WindowBottomBarTheme.Immersion
    state.mode = WindowMode.MAXIMIZE
    state.bottomBarContentColor = "#FF00FF"
  }) { modifier ->
    PreviewWindowBottomBarContent(modifier)
  }
}