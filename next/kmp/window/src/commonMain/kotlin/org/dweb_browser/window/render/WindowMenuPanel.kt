package org.dweb_browser.window.render

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.window.core.WindowController

@Composable
internal expect fun WindowMenuPanel(
  win: WindowController,
)


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
    AlertDialog(
      onDismissRequest = {
        toggleMenu(false)
      },
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
        ElevatedButton(onClick = {
          scope.launch {
            win.hideMenuPanel()
            win.close() // 增加关闭窗口
          }
        }) {
          Text("退出应用")
        }
      },
      dismissButton = {
        ElevatedButton({
          toggleMenu(false)
        }) {
          Text("关闭面板")
        }
      },
    )// AlertDialog

  }
}