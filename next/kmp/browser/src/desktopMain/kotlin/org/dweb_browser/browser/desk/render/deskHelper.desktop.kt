package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.compose.hoverEvent


actual fun desktopGridLayout(): DesktopGridLayout =
  DesktopGridLayout(
    cells = GridCells.Adaptive(100.dp),
    insets = WindowInsets(top = 24.dp, left = 24.dp, right = 24.dp),
    horizontalSpace = 8.dp,
    verticalSpace = 16.dp,
  )

actual fun desktopIconSize(): IntSize = IntSize(64, 64)

actual fun taskBarCloseButtonUsePopUp() = false

@Composable
@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.desktopAppItemActions(
  onHoverStart: () -> Unit,
  onHoverEnd: () -> Unit,
  onDoubleTap: () -> Unit,
  onOpenApp: () -> Unit,
  onOpenAppMenu: () -> Unit,
) =
  this.onClick(
    matcher = PointerMatcher.mouse(PointerButton.Primary),
    onClick = onOpenApp,
    onDoubleClick = onDoubleTap,
    onLongClick = onOpenAppMenu,
  ).onClick(
    matcher = PointerMatcher.mouse(PointerButton.Secondary),
    onDoubleClick = onDoubleTap,
    onClick = onOpenAppMenu,
  ).hoverEvent(onEnter = onHoverStart, onExit = onHoverEnd)

actual fun canSupportModifierBlur(): Boolean = true