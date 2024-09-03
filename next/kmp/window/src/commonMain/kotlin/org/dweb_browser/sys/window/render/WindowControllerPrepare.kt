package org.dweb_browser.sys.window.render

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.iosTween
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.sys.window.core.LocalWindowController
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.helper.LocalWindowControllerTheme
import org.dweb_browser.sys.window.helper.LocalWindowFrameStyle
import org.dweb_browser.sys.window.helper.LocalWindowLimits
import org.dweb_browser.sys.window.helper.WindowLimits
import org.dweb_browser.sys.window.helper.buildTheme
import org.dweb_browser.sys.window.helper.calcWindowFrameStyle
import org.dweb_browser.sys.window.helper.watchedState

@Composable
fun WindowController.Prepare(
  winMaxWidth: Float,
  winMaxHeight: Float,
  winMinWidth: Float = winMaxWidth * 0.2f,
  winMinHeight: Float = winMaxHeight * 0.2f,
  minScale: Double = 0.3,
  content: @Composable () -> Unit,
) {
  val win = this;
  win._pureViewControllerState.value = LocalPureViewController.current

  /**
   * 窗口是否在移动中
   */
  val inMove by win.inMove

  val limits = WindowLimits(
    minWidth = winMinWidth,
    minHeight = winMinHeight,
    maxWidth = winMaxWidth,
    maxHeight = winMaxHeight,
    // TODO 这里未来可以开放接口配置
    minScale = minScale,
    topBarBaseHeight = 36f,
    bottomBarBaseHeight = 24f,
  )

  /**
   * 窗口边距
   */
  val winFrameStyle = win.calcWindowFrameStyle(limits)

  val theme = win.buildTheme();
  CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
    LocalCompositionChain.current.Provider(
      LocalWindowFrameStyle provides winFrameStyle,
      LocalWindowLimits provides limits,
      LocalWindowControllerTheme provides theme,
      LocalWindowController provides win,
    ) {
      /// 显示模态层，模态层不受 isVisible 的控制
      val modal by win.openingModal
      modal?.Render()

      /// 渲染窗口关闭提示，该提示不受 isVisible 的控制
      win.RenderCloseTip()

      val isVisible by win.watchedState { isVisible }

      /**
       * 窗口缩放
       *
       * 目前，它只适配了 拖动窗口时将窗口放大的动画效果
       * TODO 需要监听 winBounds 的 height/width 变化，将这个变化适配到窗口的 scaleX、scaleY 上，只有在动画完成的时候，才会正式将真正的 size 传递给内容渲染，这样可以有效避免内容频繁的resize渲染计算。这种resize有两种情况，一种是基于用户行为的resize、一种是基于接口行为的（比如最大化），所以应该统一通过监听winBounds变更来动态生成scale动画
       */
      /**
       * 窗口缩放
       *
       * 目前，它只适配了 拖动窗口时将窗口放大的动画效果
       * TODO 需要监听 winBounds 的 height/width 变化，将这个变化适配到窗口的 scaleX、scaleY 上，只有在动画完成的时候，才会正式将真正的 size 传递给内容渲染，这样可以有效避免内容频繁的resize渲染计算。这种resize有两种情况，一种是基于用户行为的resize、一种是基于接口行为的（比如最大化），所以应该统一通过监听winBounds变更来动态生成scale动画
       */
      val scaleTargetValue = if (inMove) 1.05f else if (isVisible) 1f else 0.38f
      val scale by animateFloatAsState(
        targetValue = scaleTargetValue,
        animationSpec = iosTween(durationIn = scaleTargetValue != 1f),
        label = "scale"
      )
      if (scale == 0f) {
        return@Provider
      }
      val opacity by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = iosTween(durationIn = isVisible),
        label = "opacity"
      )
      if (opacity == 0f) {
        return@Provider
      }

      content()
    }
  }
}