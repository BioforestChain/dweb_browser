package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.sys.window.core.WindowController
import kotlin.math.min

@Composable
actual fun RowScope.WindowBottomBarMenuPanel(win: WindowController) {
  val scope = rememberCoroutineScope()
  val winPadding = LocalWindowPadding.current
  val bottomBarHeight = winPadding.bottom
  val winTheme = LocalWindowControllerTheme.current
  val contentColor = winTheme.bottomContentColor
  val infoHeight = min(bottomBarHeight * 0.25f, LocalWindowLimits.current.bottomBarBaseHeight)
  val buttonRoundedSize = infoHeight * 2

  Box(
    modifier = Modifier.weight(1f).fillMaxWidth(),
  ) {
    /// 渲染菜单面板
    WindowMenuPanel(win)

    TextButton(
      onClick = {
        scope.launch { win.toggleMenuPanel() }
      },
      contentPadding = PaddingValues(0.dp),
      shape = RoundedCornerShape(buttonRoundedSize),
      modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
    ) {
      win.IconRender(
        modifier = Modifier.align(Alignment.CenterVertically).fillMaxHeight(),
        primaryColor = contentColor
      )
    }
  }
}