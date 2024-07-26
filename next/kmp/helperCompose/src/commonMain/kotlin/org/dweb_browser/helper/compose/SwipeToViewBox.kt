package org.dweb_browser.helper.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

typealias PositionalThreshold = (contentWidth: Float, backgroundWidth: Float) -> Float
typealias PositionalLimit = (contentWidth: Float, backgroundWidth: Float) -> Float

class SwipeToViewBoxState(
  initialAllowMenuButtonOpen: Boolean,
  initialAllowFocusEvent: Boolean,
  initialValue: SwipeToViewBoxValue,
  val density: Density,
  private val scope: CoroutineScope,
  val confirmValueChange: (SwipeToViewBoxValue) -> Boolean,
  val positionalLimit: PositionalThreshold,
  val positionalThreshold: PositionalThreshold,
) {
  companion object {}

  var currentValue by mutableStateOf(initialValue)
    internal set
  var targetValue by mutableStateOf(initialValue)
    internal set
  var allowMenuButtonOpen by mutableStateOf(initialAllowMenuButtonOpen)
  var allowAllowFocusEvent by mutableStateOf(initialAllowFocusEvent)

  internal val offset = Animatable(0f)
  internal var openOffset by mutableFloatStateOf(0f)

  suspend fun open(animation: Boolean = true) {
    toggle(SwipeToViewBoxValue.Open, animation)
  }

  fun openJob(animation: Boolean = true) = scope.launch { open(animation) }

  suspend fun close(animation: Boolean = true) {
    toggle(SwipeToViewBoxValue.Close, animation)
  }

  fun closeJob(animation: Boolean = true) = scope.launch { close(animation) }

  suspend fun toggle(isOpen: Boolean = !currentValue.isOpen, animation: Boolean = true) {
    toggle(SwipeToViewBoxValue.ALL.getValue(isOpen), animation)
  }

  fun toggleJob(animation: Boolean = true) = scope.launch { toggle(animation) }

  suspend fun toggle(state: SwipeToViewBoxValue, animation: Boolean = true) {
    if (state != targetValue && confirmValueChange(state)) {
      targetValue = state
    }
    val targetOffset = when (targetValue) {
      SwipeToViewBoxValue.Open -> openOffset
      SwipeToViewBoxValue.Close -> 0f
    }
    when {
      animation -> offset.animateTo(targetOffset, spring())
      else -> offset.snapTo(targetOffset)
    }

    currentValue = targetValue
  }
}

enum class SwipeToViewBoxValue(val isOpen: Boolean) {
  Open(true), Close(false),
  ;

  companion object {
    val ALL = entries.associateBy { it.isOpen }
  }
}

object SwipeToViewBoxDefaults {
  val positionalThreshold: PositionalThreshold = { contentWidth, backgroundWidth ->
    min(56f, min(contentWidth / 2, backgroundWidth / 2))
  }
  val positionalLimit: PositionalLimit = { contentWidth, backgroundWidth ->
    min(contentWidth, backgroundWidth)
  }
}

@Composable
fun rememberSwipeToViewBoxState(
  initialAllowMenuButtonOpen: Boolean = true,
  initialAllowFocusEvent: Boolean = true,
  initialValue: SwipeToViewBoxValue = SwipeToViewBoxValue.Close,
  confirmValueChange: (SwipeToViewBoxValue) -> Boolean = { true },
  positionalLimit: PositionalLimit = SwipeToViewBoxDefaults.positionalLimit,
  positionalThreshold: PositionalThreshold = SwipeToViewBoxDefaults.positionalThreshold,
): SwipeToViewBoxState {
  val density = LocalDensity.current
  val scope = rememberCoroutineScope()
  return rememberSaveable(
    saver = Saver(
      save = { it.currentValue.isOpen },
      restore = {
        SwipeToViewBoxState(
          initialAllowMenuButtonOpen = initialAllowMenuButtonOpen,
          initialAllowFocusEvent = initialAllowFocusEvent,
          initialValue = SwipeToViewBoxValue.ALL.getValue(it),
          density = density,
          scope = scope,
          confirmValueChange = confirmValueChange,
          positionalLimit = positionalLimit,
          positionalThreshold = positionalThreshold
        )
      },
    )
  ) {
    SwipeToViewBoxState(
      initialAllowMenuButtonOpen = initialAllowMenuButtonOpen,
      initialAllowFocusEvent = initialAllowFocusEvent,
      initialValue = initialValue,
      density = density,
      scope = scope,
      confirmValueChange = confirmValueChange,
      positionalLimit = positionalLimit,
      positionalThreshold = positionalThreshold
    )
  }
}

@Composable
fun SwipeToViewBox(
  state: SwipeToViewBoxState = rememberSwipeToViewBoxState(),
  modifier: Modifier = Modifier,
  backgroundContent: @Composable (progress: Float) -> Unit,
  content: @Composable (progress: Float) -> Unit,
) {
  val offsetX = state.offset
  val scope = rememberCoroutineScope()
  val dir = LocalLayoutDirection.current
  var contentWidth by remember { mutableFloatStateOf(0f) }
  var backgroundWidth by remember { mutableFloatStateOf(0f) }
  val threshold = remember(contentWidth, backgroundWidth, state) {
    state.positionalThreshold(contentWidth, backgroundWidth)
  }
  val limit = remember(contentWidth, backgroundWidth, state) {
    state.positionalLimit(contentWidth, backgroundWidth)
  }
  val progressState = remember { mutableFloatStateOf(0f) }
  remember(offsetX.value, limit) {
    progressState.value = when {
      limit == 0f -> 0f
      else -> abs(offsetX.value) / limit
    }
  }
  var minX by remember { mutableFloatStateOf(0f) }
  var maxX by remember { mutableFloatStateOf(0f) }
  remember(dir, limit, state) {
    when (dir) {
      LayoutDirection.Ltr -> {
        minX = -limit
        state.openOffset = minX
        maxX = 0f
      }

      LayoutDirection.Rtl -> {
        minX = 0f
        maxX = limit
        state.openOffset = maxX
      }
    }
  }


  /// 提供焦点的支持
  FocusableBox(modifier = modifier) {
    /// 确定currentValue和视图状态的绑定，这里无动画。用于 initValue 和 focusRequester 的绑定
    LaunchedEffect(state.currentValue) {
      when (state.currentValue) {
        SwipeToViewBoxValue.Open -> {
          focusRequester.requestFocus()
          state.open(animation = false)
        }

        SwipeToViewBoxValue.Close -> {
          focusRequester.freeFocus()
          state.close(animation = false)
        }
      }
    }
    LaunchedEffect(hasFocus) {
      if (!hasFocus) {
        state.close()
      }
    }

    /// 渲染视图，绑定手势
    Box(
      Modifier.wrapContentSize()
        /// 滑动手势的支持
        .pointerInput(progressState, threshold, limit) {
          var endJob: Job? = null
          detectHorizontalDragGestures(onDragEnd = {
            var newTargetValue = state.targetValue
            /// 判断是否到达合适的阈值
            when (state.currentValue) {
              SwipeToViewBoxValue.Close -> {
                if (progressState.value >= threshold / limit) {
                  val targetValue = SwipeToViewBoxValue.Open
                  if (state.targetValue != targetValue) {
                    newTargetValue = targetValue
                  }
                }
              }

              SwipeToViewBoxValue.Open -> {
                if (progressState.value <= 1f - (threshold / limit)) {
                  val targetValue = SwipeToViewBoxValue.Close
                  if (state.targetValue != targetValue) {
                    newTargetValue = targetValue
                  }
                }
              }
            }
            /// 打断前一个动画
            endJob?.cancel()
            endJob = scope.launch {
              state.toggle(newTargetValue)
              endJob = null
            }
          }) { _, dragAmount ->
            /// 打断前一个动画
            endJob?.also { it.cancel(); endJob = null }
            scope.launch {
              val newValue = (offsetX.value + dragAmount).coerceIn(minX, maxX)
              offsetX.snapTo(newValue)
            }
          }
        }
        /// 鼠标右键的支持
        .pointerInput(state.allowMenuButtonOpen) {
          awaitPointerEventScope {
            while (state.allowMenuButtonOpen) {
              val event = awaitPointerEvent()
              if (event.type == PointerEventType.Press) {
                val buttons = event.buttons
                if (buttons.isSecondaryPressed) {
                  scope.launch { state.open() }
                }
              }
            }
          }
        },
    ) {
      // content视图的容器，提供视图偏移和宽度计算
      Box(Modifier.zIndex(2f).wrapContentSize().graphicsLayer { translationX = offsetX.value }
        .onGloballyPositioned { contentWidth = it.size.width.toFloat() }) {
        content(progressState.value)
      }
      // background视图的容器，默认不溢出content
      Box(Modifier.zIndex(1f).matchParentSize(), contentAlignment = Alignment.CenterEnd) {
        // background视图，默认靠end位置放置，并提供宽度计算
        Box(Modifier.onGloballyPositioned { backgroundWidth = it.size.width.toFloat() }) {
          backgroundContent(progressState.value)
        }
      }
    }
  }
}
