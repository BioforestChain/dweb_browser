package org.dweb_browser.browser.desk.render


import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import squircleshape.CornerSmoothing
import squircleshape.SquircleShape

internal fun <T> deskAniSpec() = spring<T>(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
internal fun <T> deskAniSpec1() = tween<T>(5000)

internal fun deskSquircleShape() = SquircleShape(30, CornerSmoothing.Small)

data class DesktopGridLayout(
  val cells: GridCells, val insets: WindowInsets,
  val horizontalSpace: Dp, val verticalSpace: Dp,
) {
  constructor(cells: GridCells, insets: WindowInsets, space: Dp) : this(
    cells, insets, space, space
  )
}

expect fun desktopGridLayout(): DesktopGridLayout

expect fun desktopTap(): Dp
expect fun desktopBgCircleCount(): Int
expect fun desktopIconSize(): IntSize


expect fun taskBarCloseButtonLineWidth(): Float
expect fun taskBarCloseButtonUsePopUp(): Boolean


@Composable
expect fun Modifier.desktopAppItemActions(
  onOpenApp: () -> Unit,
  onOpenAppMenu: () -> Unit,
): Modifier