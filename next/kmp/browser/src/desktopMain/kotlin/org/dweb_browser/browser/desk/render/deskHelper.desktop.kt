package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp


actual fun desktopGridLayout(): DesktopGridLayout =
  DesktopGridLayout(
    cells = GridCells.Adaptive(100.dp),
    insets = WindowInsets(left = 24.dp, right = 24.dp),
    horizontalSpace = 8.dp,
    verticalSpace = 16.dp,
  )

actual fun desktopTap(): Dp = 20.dp

actual fun desktopBgCircleCount(): Int = 12

actual fun desktopIconSize(): IntSize = IntSize(64, 64)

actual fun taskBarCloseButtonLineWidth() = 2f

actual fun taskBarCloseButtonUsePopUp() = false

@Composable
@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.desktopAppItemActions(onOpenApp: () -> Unit, onOpenAppMenu: () -> Unit) =
  this.onClick(
    matcher = PointerMatcher.mouse(PointerButton.Primary),
    onClick = onOpenApp,
    onLongClick = onOpenAppMenu,
  ).onClick(
    matcher = PointerMatcher.mouse(PointerButton.Secondary),
    onClick = onOpenAppMenu,
  )
