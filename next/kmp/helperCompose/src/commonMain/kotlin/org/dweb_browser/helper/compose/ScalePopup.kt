package org.dweb_browser.helper.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.util.fastRoundToInt

@Composable
fun ScalePopupPlaceholder(
  scale: Float,
  modifier: Modifier = Modifier,
  content: @Composable @UiComposable () -> Unit,
) {
  Box(modifier.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)

    layout(placeable.width, placeable.height) {
      placeable.place(0, 0)
    }
  }) {
    LocalCompositionChain.current.Provider(LocalePopupContentScale provides scale) {
      content()
    }
  }
}

val LocalePopupContentScale = compositionChainOf<Float>("PopupContentScale")

/**
 * 用于弹出图层的缩放
 * 用在图层内部的内容绘制的缩放，因为弹出图层是不能直接控制容器的大小的，而是需要控制内容的大小
 * 也可以用在放置图层的地方，比如 Menu 会需要一个锚地，这个锚地会有一个边界需要被缩放
 */
@Composable
fun ScalePopupContent(
  scale: Float = LocalePopupContentScale.current,
  modifier: Modifier = Modifier,
  content: @Composable @UiComposable () -> Unit,
) {
  Layout(
    modifier = modifier.graphicsLayer {
      transformOrigin = TransformOrigin(0f, 0f)
      scaleX = scale
      scaleY = scale
    },
    content = content
  ) { measurables, constraints ->
    val placeables = measurables.map { measurable ->
      measurable.measure(
        constraints.copy(
          maxHeight = (constraints.maxHeight / scale).fastRoundToInt(),
          maxWidth = (constraints.maxWidth / scale).fastRoundToInt(),
        )
      )
    }
    val layoutWidth = placeables.maxOfOrNull { it.width } ?: 0
    val layoutHeight = placeables.maxOfOrNull { it.height } ?: 0
    layout(
      width = (layoutWidth * scale).fastRoundToInt(),
      height = (layoutHeight * scale).fastRoundToInt(),
    ) {
      for (placeable in placeables) {
        placeable.place(0, 0)
      }
    }
  }
}