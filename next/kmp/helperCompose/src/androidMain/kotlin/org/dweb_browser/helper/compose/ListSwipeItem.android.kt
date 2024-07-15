package org.dweb_browser.helper.compose

import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

// TODO 由于版本androidx.compose 升级为 1.2.0-beta1 但是jetpack-compose版本没有出来，临时增加
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
    background = { progress, current, target ->
      val newProgress = if (progress == 1.0f && current == target) {
        if (current == AnchoredDragValue.Start) 1f else 0f
      } else if (current == AnchoredDragValue.Start) {
        1f - progress
      } else {
        progress
      }

      Box(
        modifier = Modifier.size(72.dp).offset {
          IntOffset(x = (72.dp.toPx() * newProgress).toInt(), y = 0)
        }.background(Color.Red).clickable { onRemove() },
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = CommonI18n.delete(),
          color = MaterialTheme.colorScheme.background,
          textAlign = TextAlign.Center
        )
      }
    },
    content = content
  )
}

enum class AnchoredDragValue {
  Start, Center, End;
}

/**
 * @param dragValues 目前最多可以支持两个按键，
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnchoredDragBox(
  modifier: Modifier = Modifier,
  draggableSize: Dp = 72.dp,
  dragValues: Set<AnchoredDragValue> = setOf(AnchoredDragValue.Start, AnchoredDragValue.End),
  background: @Composable RowScope.(Float, AnchoredDragValue, AnchoredDragValue) -> Unit,
  content: @Composable RowScope.() -> Unit,
) {
  val density = LocalDensity.current
  val draggableSizePx = with(LocalDensity.current) { draggableSize.toPx() }
  val state = remember {
    AnchoredDraggableState(
      initialValue = AnchoredDragValue.Start,
      positionalThreshold = { distance: Float -> distance * 0.5f }, // 触发位置的阈值，这里设置0.5即中间位置时切换到下一个状态
      velocityThreshold = { with(density) { 125.dp.toPx() } }, // 拖动速度必须超过该阈值才能设置到下一个状态
      snapAnimationSpec = TweenSpec(100),
      // TODO: 未验证 decayAnimationSpec 设置
      decayAnimationSpec = SplineBasedFloatDecayAnimationSpec(density).generateDecayAnimationSpec()
    )
  }

  DisposableEffect(state) {
    state.updateAnchors(
      DraggableAnchors {
        dragValues.forEach { anchoredDragValue ->
          when (anchoredDragValue) {
            AnchoredDragValue.Start -> AnchoredDragValue.Start at 0f
            AnchoredDragValue.Center -> AnchoredDragValue.Center at -draggableSizePx / 2f
            AnchoredDragValue.End -> AnchoredDragValue.End at -draggableSizePx
          }
        }
      }
    )
    onDispose { }
  }

  Box(modifier = modifier) {
    Row(modifier = Modifier.align(Alignment.TopEnd)) {
      background(state.progress, state.currentValue, state.targetValue)
    }
    Row(modifier = Modifier.fillMaxWidth()
      .offset { IntOffset(x = state.requireOffset().toInt(), y = 0) }
      .anchoredDraggable(state, Orientation.Horizontal)) {
      content()
    }
  }
}

