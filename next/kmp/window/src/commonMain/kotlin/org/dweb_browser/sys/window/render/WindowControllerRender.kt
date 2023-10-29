package org.dweb_browser.sys.window.render

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.dweb_browser.helper.compose.iosTween
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.core.windowAdapterManager

@Composable
fun WindowController.Render(
  modifier: Modifier = Modifier, maxWinWidth: Float, maxWinHeight: Float
) {
  val win = this;
  /**
   * 窗口是否在移动中
   */
  val inMove by win.inMove

  val limits = WindowLimits(
    minWidth = maxWinWidth * 0.2f,
    minHeight = maxWinHeight * 0.2f,
    maxWidth = maxWinWidth,
    maxHeight = maxWinHeight,
    // TODO 这里未来可以开放接口配置
    minScale = 0.3,
    topBarBaseHeight = 36f,
    bottomBarBaseHeight = 24f,
  )

  /**
   * 窗口大小
   */
  val winBounds = win.calcWindowBoundsByLimits(limits);

  /**
   * 窗口边距
   */
  val winPadding = win.calcWindowPaddingByLimits(limits)

  /**
   * 窗口层级
   */
  val zIndex by win.watchedState { zIndex }

  /**
   * 窗口海拔阴影
   */
  val elevation by animateFloatAsState(
    targetValue = (if (inMove) 20f else 1f) + zIndex,
    animationSpec = tween(durationMillis = if (inMove) 250 else 500),
    label = "elevation"
  )

  val theme = win.buildTheme();
  CompositionLocalProvider(
    LocalContentColor provides MaterialTheme.colorScheme.onPrimary,
    LocalWindowPadding provides winPadding,
    LocalWindowLimits provides limits,
    LocalWindowControllerTheme provides theme,
    LocalWindowController provides win,
  ) {
    /// 显示模态层，模态层不受 isVisible 的控制
    val modal by win.openingModal
    modal?.Render()

    /// 渲染窗口关闭提示，该提示不受 isVisible 的控制
    win.RenderCloseTip()

    val isVisible by win.watchedState { isVisible() }

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
      return@CompositionLocalProvider
    }
    val opacity by animateFloatAsState(
      targetValue = if (isVisible) 1f else 0f,
      animationSpec = iosTween(durationIn = isVisible),
      label = "opacity"
    )
    if (opacity == 0f) {
      return@CompositionLocalProvider
    }
    /// 开始绘制窗口
    Box(
      modifier = with(winBounds) {
        modifier
          .offset(x.dp, y.dp)
          .size(width.dp, height.dp)
      }
        .graphicsLayer {
          alpha = opacity
          scaleX = scale
          scaleY = scale
        }
        .shadow(
          elevation = elevation.dp, shape = winPadding.boxRounded.toRoundedCornerShape()
        ),
    ) {
      //#region 窗口内容
      Column(
        Modifier
          .background(theme.winFrameBrush)
          .clip(winPadding.boxRounded.toRoundedCornerShape())
          .clickable {
            win.emitFocusOrBlur(true)
          }) {
        /// 标题栏
        WindowTopBar(win)
        /// 显示内容
        Box(
          Modifier
            .weight(1f)
            .zIndex(2f)
            .padding(start = winPadding.left.dp, end = winPadding.right.dp)// TODO 这里要注意布局方向
        ) {
          val windowRenderScope = remember(limits, winPadding) {
            WindowRenderScope(
              winPadding.contentBounds.width,
              winPadding.contentBounds.height,
              win.calcContentScale(limits, winPadding)
            )
          }
          windowAdapterManager.Renderer(
            win.state.constants.wid,
            windowRenderScope,
            Modifier.clip(winPadding.contentRounded.toRoundedCornerShape())
          )
        }
        /// 显示底部控制条
        WindowBottomBar(win, Modifier.zIndex(1f))
      }
      //#endregion

      /**
       * 窗口是否聚焦
       */
      val isFocus by win.watchedState { focus }

      /// 失去焦点的时候，提供 movable 的遮罩（在移动中需要确保遮罩存在）
      if (inMove or !isFocus) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (isFocus) 0f else 0.2f))
            .windowMoveAble(win)
        )
      }
    }
  }
}
