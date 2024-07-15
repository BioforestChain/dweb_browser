package org.dweb_browser.helper.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeProgress
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

// TODO 由于版本androidx.compose 升级为 1.2.0-beta1 但是jetpack-compose版本没有出来，临时增加
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
actual fun CommonSwipeDismiss(
  modifier: Modifier,
  background: @Composable RowScope.() -> Unit,
  onRemove: () -> Unit,
  content: @Composable RowScope.() -> Unit,
) {
  AnchoredDragBox(
    modifier = modifier,
    draggableSize = 72.dp,
    background = { progress ->
      val newProgress = if (progress.fraction == 1f && progress.from == progress.to) {
        0f
      } else if (progress.from == 0) {
        1f - progress.fraction
      } else {
        progress.fraction
      }
      Box(
        modifier = Modifier.size(72.dp).background(Color.Red).clickable { onRemove() },
        contentAlignment = Alignment.Center
      ) {
        Text(
          modifier = Modifier.offset { IntOffset(x = (72.dp.toPx() * newProgress).toInt(), y = 0) },
          text = CommonI18n.delete(),
          color = MaterialTheme.colorScheme.background,
          textAlign = TextAlign.Center
        )
      }
    },
    content = content
  )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AnchoredDragBox(
  modifier: Modifier = Modifier,
  draggableSize: Dp = 72.dp,
  background: @Composable RowScope.(SwipeProgress<Int>) -> Unit,
  content: @Composable RowScope.() -> Unit,
) {
  val density = LocalDensity.current
  val draggableSizePx = with(density) { draggableSize.toPx() }
  val swipeAbleState = rememberSwipeableState(0)
  val anchors = mapOf(0f to 0, -draggableSizePx to 1) // Maps anchor points (in px) to states

  Box(modifier = modifier) {
    Row(Modifier.align(Alignment.TopEnd)) { background(swipeAbleState.progress) }
    Row(
      modifier = Modifier.fillMaxWidth()
        .offset { IntOffset(swipeAbleState.offset.value.toInt(), 0) }
        .swipeable(
          state = swipeAbleState,
          anchors = anchors,
          thresholds = { _, _ -> FractionalThreshold(0.3f) },
          orientation = Orientation.Horizontal
        )
    ) {
      content()
    }
  }
}