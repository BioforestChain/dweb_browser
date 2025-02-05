package org.dweb_browser.dwebview

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowScope
import com.teamdev.jxbrowser.browser.callback.ShowContextMenuCallback
import com.teamdev.jxbrowser.dsl.register
import com.teamdev.jxbrowser.dsl.removeCallback
import com.teamdev.jxbrowser.view.compose.BrowserView
import org.dweb_browser.dwebview.engine.ContextMenuAction
import org.dweb_browser.dwebview.engine.contextMenuSeparator
import org.dweb_browser.dwebview.engine.getContextMenuItems
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.asDesktop
import java.awt.Window

@Composable
actual fun IDWebView.Render(
  modifier: Modifier,
  onCreate: (suspend IDWebView.() -> Unit)?,
  onDispose: (suspend IDWebView.() -> Unit)?,
) {
  require(this is DWebView)

  val window by LocalPureViewController.current.asDesktop().composeWindowAsState()
  remember {
    object : WindowScope {
      override val window: Window
        get() = window
    }
  }.BrowserView(viewEngine.browser, modifier)

  val items = remember { viewEngine.browser.getContextMenuItems() }
  if (items.isNotEmpty()) {
    /// 右键菜单控制
    var menuShowed by remember { mutableStateOf(false) }

    // 渲染自定义菜单
    DropdownMenu(
      expanded = menuShowed, // Controls menu's visibility.
      onDismissRequest = { menuShowed = false },
    ) {
      for (item in items) {
        when (item) {
          is ContextMenuAction -> DropdownMenuItem(text = {
            Text(item.text)
          }, onClick = {
            item.onClick()
            menuShowed = false
          })

          contextMenuSeparator -> HorizontalDivider()
        }
      }
    }

    /// 监听打开菜单栏
    DisposableEffect(Unit) {
      // Register a callback to display the menu
      // upon right-clicking on a web page.
      viewEngine.browser.register(ShowContextMenuCallback { _, tell ->
        menuShowed = true
        tell.close() // We don't need its actions.
      })

      onDispose {
        // Remove the callback when it is no longer needed.
        viewEngine.browser.removeCallback<ShowContextMenuCallback>()
      }
    }
  }
}
