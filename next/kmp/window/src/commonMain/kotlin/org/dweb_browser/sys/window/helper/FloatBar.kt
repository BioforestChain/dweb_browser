package org.dweb_browser.sys.window.helper

import androidx.compose.animation.core.animateOffset
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.clamp
import org.dweb_browser.helper.compose.collectAsMutableState
import org.dweb_browser.helper.getValue
import org.dweb_browser.helper.platform.LocalPureViewBox
import org.dweb_browser.helper.setValue
import squircleshape.CornerSmoothing
import squircleshape.SquircleShape

class FloatBarContext(val draggableDelegate: DraggableDelegate)

/**
 * FloatBar 外壳，提供定位渲染的功能
 * 不提供 默认的手势拖拽能力，如果是纯粹的compose中使用，可以使用 FloatBarMover，否则按需求实现 mover
 */
@Composable
fun FloatBarShell(
  state: FloatBarState,
  draggableDelegate: DraggableDelegate = remember { DraggableDelegate() },
  modifier: Modifier = Modifier,
  isDark: Boolean = isSystemInDarkTheme(),
  effectBounds: @Composable Modifier.(PureRect) -> Modifier = { bounds ->
    this.size(bounds.width.dp, bounds.height.dp)
      .offset(x = bounds.x.dp, y = bounds.y.dp)
  },
  moverContent: @Composable FloatBarContext.(modifier: Modifier) -> Unit,
) {
  val viewBox = LocalPureViewBox.current
  val displaySize by produceState(Size.Zero) { value = viewBox.getDisplaySize() }

  val screenWidth = displaySize.width
  val screenHeight = displaySize.height
  val safePadding = WindowInsets.safeDrawing.asPaddingValues();
  val layoutDirection = LocalLayoutDirection.current
  val safeBounds = remember(screenWidth, screenHeight, layoutDirection, safePadding) {
    val topPadding = safePadding.calculateTopPadding().value
    val leftPadding = safePadding.calculateLeftPadding(layoutDirection).value
    state.layoutTopPadding = topPadding
    state.layoutLeftPadding = leftPadding
    SafeBounds(
      top = topPadding,
      left = leftPadding,
      bottom = screenHeight - safePadding.calculateBottomPadding().value,
      right = screenWidth - safePadding.calculateRightPadding(layoutDirection).value,
    )
  }
  var boxX by state.layoutXFlow.collectAsMutableState()
  var boxY by state.layoutYFlow.collectAsMutableState()
  val boxWidth by state.layoutWidthFlow.collectAsState()
  val boxHeight by state.layoutHeightFlow.collectAsState()
  var boxDragging by state.draggingFlow.collectAsMutableState()
  val setBoxX = remember(safeBounds) {
    { toX: Float ->
      boxX = clamp(safeBounds.left, toX, safeBounds.right - boxWidth)
    }
  }

  val setBoxY = remember(safeBounds) {
    { toY: Float ->
      boxY = clamp(safeBounds.top, toY, safeBounds.bottom - boxHeight)
    }
  }
  if (boxX.isNaN()) {
    setBoxX(safeBounds.right)
  }
  if (boxY.isNaN()) {
    setBoxY(safeBounds.vCenter * 0.618f)
  }
  val transition = updateTransition(targetState = Offset(boxX, boxY), label = "")
  val boxOffset = transition.animateOffset(label = "") { _ ->
    Offset(boxX, boxY)
  }.value

  /**
   * 监听 safeBounds,boxWidth, boxHeight 发生改变，重新进行贴遍处理
   * 这里不需要监听 boxX，boxY
   */
  LaunchedEffect(safeBounds, boxWidth, boxHeight) {
    // Y做防止溢出处理
    setBoxY(boxY)
    when {
      // 如果在拖动中，X做防止溢出处理
      boxDragging -> setBoxX(boxX)
      // 如果不是拖动中，X只需要做贴边处理
      else -> setBoxX(if (boxX > safeBounds.hCenter) safeBounds.right else safeBounds.left)
    }
  }

  remember(setBoxX, setBoxY, safeBounds) {
    draggableDelegate.dragStartCallbacks += "floatBar" to { boxDragging = true }
    draggableDelegate.dragCallbacks += "floatBar" to { dragAmount ->
      setBoxX(boxX + dragAmount.x)
      setBoxY(boxY + dragAmount.y)
    }
    draggableDelegate.dragEndCallbacks += "floatBar" to {
      boxDragging = false
      // 处理贴边
      setBoxX(if (boxX > safeBounds.hCenter) safeBounds.right else safeBounds.left)
    }
  }

  val color = remember(isDark) {
    when {
      isDark -> Color.White.copy(alpha = 0.45f)
      else -> Color.Black.copy(alpha = 0.2f)
    }
  }
  val ctx = remember(draggableDelegate) { FloatBarContext(draggableDelegate) }
  ctx.moverContent(
    modifier
      .zIndex(1000f).effectBounds(
        PureRect(
          x = boxOffset.x,
          y = boxOffset.y,
          width = boxWidth,
          height = boxHeight,
        )
      )
      .background(color, floatBarDefaultShape)
  )
}

val floatBarDefaultShape = SquircleShape(16.dp, CornerSmoothing.Small)

/**
 * FloatBar 拖拽器，提供手势拖拽的功能
 */
@Composable
fun FloatBarMover(
  draggableDelegate: DraggableDelegate,
  modifier: Modifier,
  content: @Composable () -> Unit,
) {
  Box(
    modifier.pointerInput(draggableDelegate) {
      detectDragGestures(onDragEnd = draggableDelegate.onDragEnd,
        onDragCancel = draggableDelegate.onDragEnd,
        onDragStart = { draggableDelegate.onDragStart(it / density) },
        onDrag = { _, dragAmount ->
          draggableDelegate.onDrag(dragAmount / density)
        })
    }, contentAlignment = Alignment.Center
  ) {
    content()
  }
}

private class SafeBounds(
  val left: Float,
  val top: Float,
  val right: Float,
  val bottom: Float,
) {
  val hCenter get() = left + (right - left) / 2
  val vCenter get() = top + (bottom - top) / 2
}

class DraggableDelegate() {
  val dragStartCallbacks = mutableMapOf<String, (startPos: Offset) -> Unit>()
  val onDragStart: (startPos: Offset) -> Unit = { startPos ->
    dragStartCallbacks.values.forEach { it(startPos) }
  }

  val dragCallbacks = mutableMapOf<String, (dragAmount: Offset) -> Unit>()
  val onDrag: (dragAmount: Offset) -> Unit = { dragAmount ->
    dragCallbacks.values.forEach { it(dragAmount) }
  }

  val dragEndCallbacks = mutableMapOf<String, () -> Unit>()
  val onDragEnd: () -> Unit = {
    dragEndCallbacks.values.forEach { it() }
  }
}


/**
 * 用于和 Service 之间的交互，显示隐藏等操作
 */
class FloatBarState {
  val layoutXFlow = MutableStateFlow(Float.NaN)
  var layoutX by layoutXFlow
  val layoutYFlow = MutableStateFlow(Float.NaN)
  var layoutY by layoutYFlow
  val layoutWidthFlow = MutableStateFlow(55f)
  var layoutWidth by layoutWidthFlow
  val layoutHeightFlow = MutableStateFlow(55f)
  var layoutHeight by layoutHeightFlow
  val layoutTopPaddingFlow = MutableStateFlow(0f)
  var layoutTopPadding by layoutTopPaddingFlow
  val layoutLeftPaddingFlow = MutableStateFlow(0f)
  var layoutLeftPadding by layoutLeftPaddingFlow
  val floatActivityStateFlow = MutableStateFlow(false)
  var floatActivityState by floatActivityStateFlow

  /**
   * 专门用于桌面端的拖拽
   */
  val draggingFlow = MutableStateFlow(false)
  var dragging by draggingFlow
}
