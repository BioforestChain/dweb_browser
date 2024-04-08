package org.dweb_browser.helper.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.onClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton

// TODO 桌面端应该改变交互方式，比如使用鼠标右键而不是通过swipe
@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun CommonSwipeDismiss(
  modifier: Modifier,
  background: @Composable RowScope.() -> Unit,
  onRemove: () -> Unit,
  content: @Composable RowScope.() -> Unit,
) {
  var showDropMenu by remember { mutableStateOf(false) }
  Row(
    modifier = modifier.onClick(
      matcher = PointerMatcher.mouse(PointerButton.Secondary), // Right Mouse Button
      onClick = { showDropMenu = true },
    )
  ) {
    content()
  }

  DropdownMenu(expanded = showDropMenu, onDismissRequest = { showDropMenu = false }) {
    DropdownMenuItem(
      text = { Text(text = SwipeI18nResource.delete()) },
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = SwipeI18nResource.delete()
        )
      },
      onClick = onRemove
    )
  }
}

