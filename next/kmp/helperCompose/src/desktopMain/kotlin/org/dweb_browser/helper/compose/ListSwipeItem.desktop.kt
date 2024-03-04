package org.dweb_browser.helper.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.helper.WARNING

// TODO 桌面端应该改变交互方式，比如使用鼠标右键而不是通过swipe
@Composable
actual fun CommonSwipeDismiss(
  modifier: Modifier,
  background: @Composable RowScope.() -> Unit,
  onRemove: () -> Unit,
  content: @Composable RowScope.() -> Unit,
) {
  WARNING("Not yet implement Desktop CommonSwipeDismiss")
}

