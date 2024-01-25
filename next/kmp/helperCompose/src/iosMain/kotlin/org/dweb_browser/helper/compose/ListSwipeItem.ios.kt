package org.dweb_browser.helper.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.helper.WARNING

// TODO 由于版本androidx.compose 升级为 1.2.0-beta1 但是jetpack-compose版本没有出来，临时增加
@Composable
actual fun CommonSwipeDismiss(
  modifier: Modifier,
  background: @Composable RowScope.() -> Unit,
  dismissContent: @Composable RowScope.() -> Unit,
  onRemove: () -> Unit
) {
  WARNING("Not yet implemented CommonSwipeDismiss")
  Row { dismissContent() }
}