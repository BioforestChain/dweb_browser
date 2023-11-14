package org.dweb_browser.browser.desk

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
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.clamp
import org.dweb_browser.helper.platform.asIos
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.UIKit.addChildViewController
import platform.WebKit.WKWebViewConfiguration

actual suspend fun ITaskbarView.Companion.create(taskbarController: TaskbarController): ITaskbarView =
  TaskbarView(taskbarController)

class TaskbarView(private val taskbarController: TaskbarController) :
  ITaskbarView(taskbarController) {
  @OptIn(ExperimentalForeignApi::class)
  override val taskbarDWebView by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    require(taskbarController is TaskbarController)
    DWebView(DWebViewEngine(
//      frame = taskbarController.platformContext!!.iosController.view.frame,
      frame = CGRectMake(0.0, 0.0, 0.0, 0.0),
      remoteMM = taskbarController.deskNMM,
      options = DWebViewOptions(
        url = taskbarController.getTaskbarUrl().toString(),
//        onDetachedFromWindowStrategy = DWebViewOptions.DetachedFromWindowStrategy.Ignore,
      ),
      WKWebViewConfiguration()
    ).also {
      it.setBackgroundColor(UIColor.fromColorInt(Color.Transparent.toArgb()))
    })
  }

  @Composable
  override fun FloatWindow() {
    val isActivityMode by state.composableHelper.stateOf { floatActivityState }
    if (isActivityMode) {
      val uiViewController = LocalUIViewController.current
      LaunchedEffect(state.composableHelper) {
        uiViewController.addChildViewController(taskbarController.platformContext!!.asIos().uiViewController())
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
//        AndroidView(
//          factory = {
//            taskbarDWebView.let { dwebview ->
//              val webView = dwebview.getAndroidWebViewEngine()
//              webView.parent?.let { parent ->
//                (parent as ViewGroup).removeView(webView)
//              }
//              webView.clearFocus()
//              webView
//            }
//          }, modifier = Modifier
//        )
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

fun UIColor.Companion.fromColorInt(color: Int): UIColor {
  return UIColor(red = ((color ushr 16) and 0xFF) / 255.0, green = ((color ushr 8) and 0xFF) / 255.0, blue = ((color) and 0xFF) / 255.0, alpha = 1.0)
}