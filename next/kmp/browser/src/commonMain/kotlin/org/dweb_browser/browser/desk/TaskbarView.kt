package org.dweb_browser.browser.desk

import androidx.compose.animation.core.animateOffset
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.dweb_browser.browser.desk.render.NewTaskbarView
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.clamp
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch

expect suspend fun ITaskbarView.Companion.create(taskbarController: TaskbarController): ITaskbarView

abstract class ITaskbarView(private val taskbarController: TaskbarController) {
  val state = taskbarController.state

  abstract val taskbarDWebView: IDWebView

  companion object {}


  data class SafeBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
  ) {
    val hCenter get() = left + (right - left) / 2
    val vCenter get() = top + (bottom - top) / 2
  }

  @Composable
  abstract fun TaskbarViewRender(draggableHelper: DraggableHelper, modifier: Modifier)

  @Composable
  open fun TaskbarViewRenderNewVersion(draggableHelper: DraggableHelper, modifier: Modifier) {
    NewTaskbarView(taskbarController, draggableHelper, modifier)
  }

  /**
   * 普通的浮动窗口，背景透明
   */
  @Composable
  open fun NormalFloatWindow() {
    BoxWithConstraints(Modifier.background(Color.Transparent)) {
      val screenWidth = maxWidth.value
      val screenHeight = maxHeight.value
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
      var boxX by state.composableHelper.mutableStateOf(getter = { layoutX },
        setter = { layoutX = it })
      var boxY by state.composableHelper.mutableStateOf(getter = { layoutY },
        setter = { layoutY = it })
      val boxWidth by state.composableHelper.stateOf { layoutWidth }
      val boxHeight by state.composableHelper.stateOf { layoutHeight }
      var boxDraging by state.composableHelper.mutableStateOf(getter = { taskbarDragging },
        setter = { taskbarDragging = it })
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
          boxDraging -> setBoxX(boxX)
          // 如果不是拖动中，X只需要做贴边处理
          else -> setBoxX(if (boxX > safeBounds.hCenter) safeBounds.right else safeBounds.left)
        }
      }

      val draggableHelper = remember(setBoxX, setBoxY, safeBounds) {
        DraggableHelper(onDragStart = {
          boxDraging = true
        }, onDrag = { dragAmount ->
          setBoxX(boxX + dragAmount.x)
          setBoxY(boxY + dragAmount.y)
        }, onDragEnd = {
          boxDraging = false
          // 处理贴边
          setBoxX(if (boxX > safeBounds.hCenter) safeBounds.right else safeBounds.left)
        })
      }

      val modifier = Modifier
        .zIndex(1000f)
        .size(boxWidth.dp, boxHeight.dp)
        .offset(x = boxOffset.x.dp, y = boxOffset.y.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color.Black.copy(alpha = 0.2f))

      if (remember { envSwitch.isEnabled(ENV_SWITCH_KEY.DESKTOP_STYLE_COMPOSE) }) {
        TaskbarViewRenderNewVersion(draggableHelper, modifier)
      } else {
        TaskbarViewRender(draggableHelper, modifier)
      }
    }
  }

  class DraggableHelper(
    val onDragStart: (startPos: Offset) -> Unit,
    val onDrag: (dragAmount: Offset) -> Unit,
    val onDragEnd: () -> Unit,
  )

  @Composable
  abstract fun FloatWindow()
}
