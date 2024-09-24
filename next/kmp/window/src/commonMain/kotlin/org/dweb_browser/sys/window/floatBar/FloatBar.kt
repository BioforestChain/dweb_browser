package org.dweb_browser.sys.window.floatBar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateOffset
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.helper.PureBounds
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.clamp
import org.dweb_browser.helper.compose.collectAsMutableState
import org.dweb_browser.helper.getValue
import org.dweb_browser.helper.platform.rememberDisplaySize
import org.dweb_browser.helper.platform.rememberSafeAreaInsets
import org.dweb_browser.helper.setValue
import squircleshape.CornerSmoothing
import squircleshape.SquircleShape

class FloatBarContext(
  val draggableDelegate: DraggableDelegate,
  val safeBounds: SafeBounds,
  val backgroundColor: Color,
)

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
  displaySize: Size = rememberDisplaySize(),
  safePadding: PureBounds = rememberSafeAreaInsets(),
  effectBounds: @Composable Modifier.(PureRect) -> Modifier = { bounds ->
    this.requiredSize(bounds.width.dp, bounds.height.dp).offset(x = bounds.x.dp, y = bounds.y.dp)
  },
  moverContent: @Composable FloatBarContext.(modifier: Modifier) -> Unit,
) {
  if (displaySize == Size.Zero) {
    return
  }

  val screenWidth = displaySize.width
  val screenHeight = displaySize.height
  val layoutDirection = LocalLayoutDirection.current
  val safeBounds = remember(screenWidth, screenHeight, layoutDirection, safePadding) {
    state.layoutTopPadding = safePadding.top
    state.layoutLeftPadding = safePadding.left
    SafeBounds(
      top = safePadding.top,
      left = safePadding.left,
      bottom = screenHeight - safePadding.bottom,
      right = screenWidth - safePadding.right,
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

  val backgroundColor = floatBarBackgroundColor(
    isDark = isDark,
    getAlpha = state.backgroundAlphaGetterFlow.collectAsState().value,
  )
  val ctx = remember(draggableDelegate, safeBounds, backgroundColor) {
    FloatBarContext(draggableDelegate, safeBounds, backgroundColor)
  }
  ctx.moverContent(
    modifier.zIndex(1000f).effectBounds(
      PureRect(
        x = boxOffset.x,
        y = boxOffset.y,
        width = boxWidth,
        height = boxHeight,
      )
    ).background(
      color = backgroundColor,
      shape = floatBarDefaultShape,
    )
  )
}

@Composable
private fun floatBarBackgroundColor(
  isDark: Boolean,
  getAlpha: ((Float) -> Float)? = null,
) = animateColorAsState(remember(isDark, getAlpha) {
  when {
    isDark -> Color.White.copy(alpha = 0.45f.let { getAlpha?.invoke(it) ?: it })
    else -> Color.Black.copy(alpha = 0.2f.let { getAlpha?.invoke(it) ?: it })
  }
}).value

fun Modifier.floatBarBackground(
  isDark: Boolean,
  shape: Shape = floatBarDefaultShape,
  getAlpha: ((Float) -> Float)? = null,
) = this.composed {
  this.background(animateColorAsState(remember(isDark, getAlpha) {
    when {
      isDark -> Color.White.copy(alpha = 0.45f.let { getAlpha?.invoke(it) ?: it })
      else -> Color.Black.copy(alpha = 0.2f.let { getAlpha?.invoke(it) ?: it })
    }
  }).value, shape)
}

val floatBarDefaultShape = SquircleShape(16.dp, CornerSmoothing.Small)

/**
 * FloatBar 拖拽器，提供手势拖拽的功能
 */
@Composable
fun FloatBarMover(
  draggableDelegate: DraggableDelegate,
  modifier: Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier.pointerInput(draggableDelegate) {
      detectDragGestures(
        onDragEnd = draggableDelegate.onDragEnd,
        onDragCancel = draggableDelegate.onDragEnd,
        onDragStart = { draggableDelegate.onDragStart(it / density) },
        onDrag = { _, dragAmount ->
          draggableDelegate.onDrag(dragAmount / density)
        },
      )
    }
  ) {
    content()
  }
}

class SafeBounds(
  val left: Float,
  val top: Float,
  val right: Float,
  val bottom: Float,
) {
  val hCenter get() = left + (right - left) / 2
  val vCenter get() = top + (bottom - top) / 2
  val height get() = bottom - top
  val width get() = right - left
  val size get() = Size(width, height)
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

  val offsetXFlow = MutableStateFlow(0)
  var offsetX by offsetXFlow

  /**
   * 专门用于桌面端的拖拽
   */
  val draggingFlow = MutableStateFlow(false)
  var dragging by draggingFlow
  val backgroundAlphaGetterFlow = MutableStateFlow<((Float) -> Float)?>(null)
  var backgroundAlphaGetter by backgroundAlphaGetterFlow
}
