package org.dweb_browser.helper.compose.reorder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.ReorderAbleItem(
  reorderAbleState: ReorderableState<*>,
  key: Any?,
  modifier: Modifier = Modifier,
  index: Int? = null,
  orientationLocked: Boolean = true,
  content: @Composable BoxScope.(isDragging: Boolean) -> Unit
) = ReorderAbleItem(
  reorderAbleState,
  key,
  modifier,
  Modifier.animateItem(),
  orientationLocked,
  index,
  content
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyGridItemScope.ReorderAbleItem(
  reorderAbleState: ReorderableState<*>,
  key: Any?,
  modifier: Modifier = Modifier,
  index: Int? = null,
  content: @Composable BoxScope.(isDragging: Boolean) -> Unit
) = ReorderAbleItem(
  reorderAbleState,
  key,
  modifier,
  Modifier.animateItem(),
  false,
  index,
  content
)

@Composable
fun ReorderAbleItem(
  state: ReorderableState<*>,
  key: Any?,
  modifier: Modifier = Modifier,
  defaultDraggingModifier: Modifier = Modifier,
  orientationLocked: Boolean = true,
  index: Int? = null,
  content: @Composable BoxScope.(isDragging: Boolean) -> Unit
) {
  val isDragging = if (index != null) {
    index == state.draggingItemIndex
  } else {
    key == state.draggingItemKey
  }
  val draggingModifier =
    if (isDragging) {
      Modifier
        .zIndex(1f)
        .graphicsLayer {
          translationX =
            if (!orientationLocked || !state.isVerticalScroll) state.draggingItemLeft else 0f
          translationY =
            if (!orientationLocked || state.isVerticalScroll) state.draggingItemTop else 0f
        }
    } else {
      val cancel = if (index != null) {
        index == state.dragCancelledAnimation.position?.index
      } else {
        key == state.dragCancelledAnimation.position?.key
      }
      if (cancel) {
        Modifier.zIndex(1f)
          .graphicsLayer {
            translationX =
              if (!orientationLocked || !state.isVerticalScroll) state.dragCancelledAnimation.offset.x else 0f
            translationY =
              if (!orientationLocked || state.isVerticalScroll) state.dragCancelledAnimation.offset.y else 0f
          }
      } else {
        defaultDraggingModifier
      }
    }
  Box(modifier = modifier.then(draggingModifier)) {
    content(isDragging)
  }
}