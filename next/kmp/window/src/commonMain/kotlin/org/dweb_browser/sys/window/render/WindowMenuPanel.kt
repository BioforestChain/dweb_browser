package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.helper.LocalWindowControllerTheme
import org.dweb_browser.sys.window.helper.watchedState

@Composable
internal expect fun WindowMenuPanel(
  win: WindowController,
)

@Composable
fun WindowController.ExitAppButton() {
  val winTheme = LocalWindowControllerTheme.current
  val scope = rememberCoroutineScope()
  ElevatedButton(
    onClick = {
      scope.launch {
        hideMenuPanel()
        closeRoot()
      }
    },
    colors = winTheme.ThemeContentButtonColors(),
  ) {
    Text("退出应用")
  }
}

@Composable
fun WindowController.CloseMenuPanelButton() {
  val winTheme = LocalWindowControllerTheme.current
  val scope = rememberCoroutineScope()
  Button(
    onClick = {
      scope.launch {
        hideMenuPanel()
      }
    },
    colors = winTheme.ThemeButtonColors(),
  ) {
    Text("关闭面板")
  }
}

@Composable
internal fun WindowMenuPanelByAlert(
  win: WindowController,
) {
  val scope = rememberCoroutineScope()
  val isShowMenuPanel by win.watchedState { showMenuPanel }
  val toggleMenu = { show: Boolean ->
    scope.launch {
      win.toggleMenuPanel(show)
    }
  }
  if (isShowMenuPanel) {
    val winTheme = LocalWindowControllerTheme.current
    AlertDialog(
      onDismissRequest = {
        toggleMenu(false)
      },
      containerColor = winTheme.themeColor,
      iconContentColor = winTheme.themeContentColor,
      textContentColor = winTheme.themeContentColor,
      titleContentColor = winTheme.themeContentColor,
      icon = {
        win.IconRender(modifier = Modifier.size(36.dp))
      },
      title = {
        val owner = win.state.constants.owner
        Text(text = owner, maxLines = 1, overflow = TextOverflow.Ellipsis)
      },
      text = {
        WindowControlPanel(win)
      },
      confirmButton = {
        win.ExitAppButton()
      },
      dismissButton = {
        win.CloseMenuPanelButton()
      },
    )// AlertDialog

  }
}