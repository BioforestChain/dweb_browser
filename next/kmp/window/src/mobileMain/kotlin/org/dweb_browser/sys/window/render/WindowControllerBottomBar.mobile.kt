package org.dweb_browser.sys.window.render

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.iosTween
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.helper.LocalWindowControllerTheme
import org.dweb_browser.sys.window.helper.LocalWindowFrameStyle
import org.dweb_browser.sys.window.helper.LocalWindowLimits
import org.dweb_browser.sys.window.helper.watchedIsMaximized
import org.dweb_browser.sys.window.helper.watchedState
import kotlin.math.min

@Composable
actual fun RowScope.WindowBottomBarMenuPanel(win: WindowController) {
  val scope = rememberCoroutineScope()
  val winTheme = LocalWindowControllerTheme.current
  val contentColor = winTheme.bottomContentColor
  val winFrameStyle = LocalWindowFrameStyle.current
  val bottomBarHeight = winFrameStyle.frameSize.bottom
  val infoHeight = min(bottomBarHeight * 0.25f, LocalWindowLimits.current.bottomBarBaseHeight)
  val isMaximized by win.watchedIsMaximized()
  val buttonRoundedSize = infoHeight * 2

  /// 菜单按钮
  if (isMaximized) {
    Box(
      modifier = Modifier.weight(1f).fillMaxHeight()
    ) {

      /// 渲染菜单面板
      WindowMenuPanel(win)

      TextButton(
        onClick = {
          scope.launch { win.toggleMenuPanel() }
        },
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(buttonRoundedSize),
        modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(),
      ) {
        /// 菜单按钮动画
        BoxWithConstraints(
          modifier = Modifier.align(Alignment.CenterVertically)
        ) {
          val isShowMenuPanel by win.watchedState { showMenuPanel }
          val closeIconOpacity by animateFloatAsState(
            targetValue = if (isShowMenuPanel) 1f else 0f,
            animationSpec = iosTween(isShowMenuPanel),
            label = "icon animation",
          )
          val size = min(maxWidth.value, maxHeight.value)
          Icon(
            Icons.Rounded.KeyboardDoubleArrowUp,
            contentDescription = "Close menu panel",
            modifier = Modifier.alpha(closeIconOpacity)
              .offset(y = ((closeIconOpacity - 1) * size / 2).dp),
            tint = contentColor,
          )
          Icon(
            Icons.Rounded.Menu,
            contentDescription = "Open menu panel",
            modifier = Modifier.alpha(1 - closeIconOpacity)
              .offset(y = (closeIconOpacity * size / 2).dp),
            tint = contentColor,
          )

        }
      }
    }
  }
}