package org.dweb_browser.browser.desk.render


import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import squircleshape.CornerSmoothing
import squircleshape.SquircleShape
import kotlin.math.max

internal fun <T> deskAniFastSpec() = spring<T>(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)
internal fun <T> deskAniSpec() = spring<T>(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
internal fun <T> deskAniSpec1() = tween<T>(3000)

internal fun deskSquircleShape() = SquircleShape(30, CornerSmoothing.Small)

/**
 * 透明度不能太小，否则会引起渲染异常，图层会会某名奇妙永远消失
 */
fun safeAlpha(alpha: Float) = max(alpha, 0.01f)

data class DesktopGridLayout(
  val cells: GridCells, val insets: WindowInsets,
  val horizontalSpace: Dp, val verticalSpace: Dp,
) {
  constructor(cells: GridCells, insets: WindowInsets, space: Dp) : this(
    cells, insets, space, space
  )
}

expect fun desktopGridLayout(): DesktopGridLayout

expect fun canSupportModifierBlur(): Boolean

@Composable
expect fun Modifier.desktopAppItemActions(
  onHoverStart: () -> Unit = {},
  onHoverEnd: () -> Unit = {},
  onDoubleTap: () -> Unit = {},
  onOpenApp: () -> Unit = {},
  onOpenAppMenu: () -> Unit = {},
): Modifier