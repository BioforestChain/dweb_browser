package info.bagen.dwebbrowser.microService.browser.desk

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.animation.core.animateOffset
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import info.bagen.dwebbrowser.App
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.DWebViewEngine
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.helper.clamp

class TaskbarView(private val taskbarController: TaskbarController) {
  val state = TaskbarState()

  val taskbarDWebView by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    DWebViewEngine(
      context = App.appContext, remoteMM = taskbarController.desktopNMM, options = DWebViewOptions(
        url = taskbarController.getTaskbarUrl().toString(),
        onDetachedFromWindowStrategy = DWebViewOptions.DetachedFromWindowStrategy.Ignore,
      )
    ).also {
      it.setBackgroundColor(Color.Transparent.toArgb())
    }
  }

  /**
   * 打开悬浮框
   */
  private fun openTaskbarActivity() = if (!state.floatActivityState) {
    state.floatActivityState = true
    true
  } else false

  private fun closeTaskbarActivity() = if (state.floatActivityState) {
    state.floatActivityState = false
    true
  } else false

  suspend fun toggleFloatWindow(openTaskbar: Boolean?): Boolean {
    val toggle = openTaskbar ?: !state.floatActivityState
    // 监听状态是否是float
    taskbarController.getFocusApp()?.let { focusApp ->
      taskbarController.stateSignal.emit(
        TaskbarController.TaskBarState(toggle, focusApp)
      )
    }
    return if (toggle) openTaskbarActivity() else closeTaskbarActivity()
  }

  private data class SafeBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
  ) {
    val hCenter get() = left + (right - left) / 2
    val vCenter get() = top + (bottom - top) / 2
  }

  @SuppressLint("IntentWithNullActionLaunch")
  @Composable
  fun FloatWindow() {
    val isActivityMode by state.composableHelper.stateOf { floatActivityState }
    if (isActivityMode) {
      LaunchedEffect(state.composableHelper) {
        App.appContext.startActivity(Intent(
          App.appContext, TaskbarActivity::class.java
        ).also { intent ->
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
          intent.putExtras(Bundle().apply {
            putString("deskSessionId", taskbarController.deskSessionId)
          })
        })
      }
      return
    }

    BoxWithConstraints {
      val screenWidth = maxWidth.value
      val screenHeight = maxHeight.value
      val density = LocalDensity.current.density
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
      fun setBoxX(toX: Float) {
        boxX = clamp(safeBounds.left, toX, safeBounds.right - boxWidth)
      }

      fun setBoxY(toY: Float) {
        boxY = clamp(safeBounds.top, toY, safeBounds.bottom - boxHeight)
      }

      if (boxX.isNaN()) {
        setBoxX(safeBounds.right)
      }
      if (boxY.isNaN()) {
        setBoxY(safeBounds.vCenter * 0.618f)
      }
      var inDrag by remember { mutableStateOf(false) }
      val transition = updateTransition(targetState = Offset(boxX, boxY), label = "")
      val boxOffset = transition.animateOffset(label = "") { _ ->
        Offset(boxX, boxY)
      }.value

      Box(modifier = Modifier
        .zIndex(1000f)
        .size(boxWidth.dp, boxHeight.dp)
        .absoluteOffset() {
          boxOffset.toIntOffset(density)
        }
        .pointerInput(Unit) {
          detectDragGestures(onDragEnd = {
            inDrag = false
            // 处理贴边
            setBoxX(if (boxX > safeBounds.hCenter) safeBounds.right else safeBounds.left)
          }, onDragStart = {
            inDrag = true
          }, onDrag = { _, dragAmount ->
            setBoxX(boxX + dragAmount.x / density)
            setBoxY(boxY + dragAmount.y / density)
          })
        }) {
        AndroidView(
          factory = {
            taskbarDWebView.also { webView ->
              webView.parent?.let { parent ->
                (parent as ViewGroup).removeView(webView)
              }
              webView.clearFocus()
            }

          }, modifier = Modifier
        )
//        // 这边屏蔽当前webview响应
//        Box(modifier = Modifier
//          .fillMaxSize()
//          .clickableWithNoEffect {
//            floatWindowState.value = false
//            taskbarDWebView.requestFocus()
//            openTaskActivity()
//          })
      }
    }
  }
}

fun Offset.toIntOffset(density: Float) = IntOffset((density * x).toInt(), (density * y).toInt())