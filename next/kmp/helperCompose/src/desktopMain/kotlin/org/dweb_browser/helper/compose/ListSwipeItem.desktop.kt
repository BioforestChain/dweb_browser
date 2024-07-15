package org.dweb_browser.helper.compose

import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

// TODO 桌面端应该改变交互方式，比如使用鼠标右键而不是通过swipe
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun CommonSwipeDismiss(
  modifier: Modifier,
  background: @Composable RowScope.() -> Unit,
  onRemove: () -> Unit,
  content: @Composable RowScope.() -> Unit,
) {
  var showDropMenu by remember { mutableStateOf(false) }
  var offset by remember { mutableStateOf(Offset.Zero) }
  val density = LocalDensity.current.density

  Row(modifier = modifier.pointerInput(Unit) {
    forEachGesture {
      awaitPointerEventScope {
        while (true) {
          val event = awaitPointerEvent()
          event.changes.forEach { change ->
            if (change.pressed && event.button == PointerButton.Secondary) {
              offset = change.position
              showDropMenu = true
            }
          }
        }
      }
    }
  }) {
    content()
    DropdownMenu(
      expanded = showDropMenu,
      onDismissRequest = { showDropMenu = false },
      offset = DpOffset((offset.x / density).dp, (offset.y / density).dp - 86.dp) // 锚点偏移量
    ) {
      DropdownMenuItem(text = { Text(text = CommonI18n.delete()) }, leadingIcon = {
        Icon(
          imageVector = Icons.Default.Delete, contentDescription = CommonI18n.delete()
        )
      }, onClick = onRemove
      )
    }
  }
}

