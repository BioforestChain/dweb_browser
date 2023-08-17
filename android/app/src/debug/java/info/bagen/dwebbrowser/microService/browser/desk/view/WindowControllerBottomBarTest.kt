package info.bagen.dwebbrowser.microService.browser.desk.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import info.bagen.dwebbrowser.microService.core.WindowBottomBarTheme
import info.bagen.dwebbrowser.microService.core.WindowMode


@Composable
fun PreviewWindowBottomBarContent(modifier: Modifier) {
  var history by remember {
    mutableIntStateOf(0)
  }
  Box(
    modifier
      .background(Color.DarkGray)
      .clickable {
        history += 1
      }) {
    BackHandler(true) {
      history -= 1
    }
    Text(
      text = "当前记录：${history}", modifier = Modifier.align(Alignment.Center)
    )
  }
}

@Preview(widthDp = 400, heightDp = 100)
@Composable
fun PreviewWindowBottomNavigationBar() {
  WindowPreviewer(config = {
    state.bottomBarTheme = WindowBottomBarTheme.Navigation
    state.bottomBarContentColor = "#FF00FF"
  }) { modifier, _, _, _ ->
    PreviewWindowBottomBarContent(modifier)
  }
}

@Preview(widthDp = 400, heightDp = 100)
@Composable
fun PreviewWindowBottomNavigationBarWithMax() {
  WindowPreviewer(config = {
    state.bottomBarTheme = WindowBottomBarTheme.Navigation
    state.mode = WindowMode.MAXIMIZE
    state.bottomBarContentColor = "#FF00FF"
  }) { modifier, _, _, _ ->
    PreviewWindowBottomBarContent(modifier)
  }
}

@Preview(widthDp = 400, heightDp = 100)
@Composable
fun PreviewWindowBottomImmersionBar() {
  WindowPreviewer(config = {
    state.bottomBarTheme = WindowBottomBarTheme.Immersion
    state.bottomBarContentColor = "#FF00FF"
  }) { modifier, _, _, _ ->
    PreviewWindowBottomBarContent(modifier)
  }
}

@Preview(widthDp = 400, heightDp = 100)
@Composable
fun PreviewWindowBottomImmersionBarWithMax() {
  WindowPreviewer(config = {
    state.bottomBarTheme = WindowBottomBarTheme.Immersion
    state.mode = WindowMode.MAXIMIZE
    state.bottomBarContentColor = "#FF00FF"
  }) { modifier, _, _, _ ->
    PreviewWindowBottomBarContent(modifier)
  }
}