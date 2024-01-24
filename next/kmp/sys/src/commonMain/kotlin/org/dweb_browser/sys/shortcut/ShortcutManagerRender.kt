package org.dweb_browser.sys.shortcut

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.dweb_browser.sys.window.core.WindowRenderScope

@Composable
fun ShortcutManagerRender(
  modifier: Modifier, windowRenderScope: WindowRenderScope, shortcutStore: ShortcutStore
) {

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text("未实现功能")
  }
}