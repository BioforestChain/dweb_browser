package info.bagen.dwebbrowser.microService.browser.nativeui.helper

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.runtime.Composable
import androidx.core.graphics.Insets

operator fun WindowInsets.plus(safeAreaCutoutInsets: WindowInsets) =
    this.add(safeAreaCutoutInsets)

@Composable
fun Insets.toWindowsInsets() = WindowInsets(
    top = top,
    left = left,
    right = right,
    bottom = bottom,
)
