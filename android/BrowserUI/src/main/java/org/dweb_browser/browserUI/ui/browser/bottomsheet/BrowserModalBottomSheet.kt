package org.dweb_browser.browserUI.ui.browser.bottomsheet

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.dweb_browser.browserUI.bookmark.clickableWithNoEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowserModalBottomSheet(
  onDismissRequest: () -> Unit,
  shape: Shape = BottomSheetDefaults.ExpandedShape,
  dragHandle: @Composable (() -> Unit) = { BottomSheetDefaults.DragHandle() },
  content: @Composable ColumnScope.() -> Unit,
) {
  val state = remember { mutableStateOf(SheetState.PartiallyExpanded) }
  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
  ) {
    key(state) {
      val density = LocalDensity.current.density
      val parentHeight = maxHeight.value * density
      val currentState = remember { mutableFloatStateOf(state.value.defaultHeight(parentHeight)) }

      val height = animateDpAsState(
        targetValue = (currentState.floatValue / density).dp, label = "",
        finishedListener = {
          if (state.value == SheetState.Hidden) {
            onDismissRequest()
          }
        }
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clickableWithNoEffect {
            currentState.floatValue = 0f
            state.value = SheetState.Hidden
          }
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(height.value)
          .align(Alignment.BottomCenter)
          .clip(shape)
          .background(MaterialTheme.colorScheme.background)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.CenterHorizontally)
            .pointerInput(currentState) {
              detectDragGestures(
                onDragEnd = {
                  if (currentState.floatValue > parentHeight * 3 / 4) {
                    currentState.floatValue = parentHeight
                    state.value = SheetState.Expanded
                  } else if (currentState.floatValue < parentHeight / 2) {
                    currentState.floatValue = 0f
                    state.value = SheetState.Hidden
                  } else {
                    currentState.floatValue = parentHeight * 2 / 3
                    state.value = SheetState.PartiallyExpanded
                  }
                },
                onDrag = { _, dragAmount ->
                  currentState.floatValue -= dragAmount.y
                }
              )
            }, contentAlignment = Alignment.TopCenter
        ) {
          dragHandle()
        }
        content()
      }
    }

  }
}