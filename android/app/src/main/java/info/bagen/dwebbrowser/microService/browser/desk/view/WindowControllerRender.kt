package info.bagen.dwebbrowser.microService.browser.desk.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.microService.browser.desk.DesktopWindowController
import info.bagen.dwebbrowser.microService.core.windowAdapterManager


@Composable
fun DesktopWindowController.Render(
  modifier: Modifier = Modifier, maxWinWidth: Float, maxWinHeight: Float
) {
  val win = this;
  /**
   * 窗口是否在移动中
   */
  val inMove by win.inMove

  /** 窗口是否在调整大小中 */
  val inResize by win.inResize

  println("[${win.id}] inMove:${inMove} inResize:${inResize}")

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
  val winEdge = win.calcWindowEdgeByLimits(limits)

  /**
   * 窗口层级
   */
  val zIndex by win.state.watchedState { zIndex }

  /**
   * 窗口海拔阴影
   */
  val elevation by animateFloatAsState(
    targetValue = (if (inMove) 20f else 1f) + zIndex,
    animationSpec = tween(durationMillis = if (inMove) 250 else 500),
    label = "elevation"
  )

  /**
   * 窗口缩放
   */
  val scale by animateFloatAsState(
    targetValue = if (inMove) 1.05f else 1f,
    animationSpec = tween(durationMillis = if (inMove) 250 else 500),
    label = "scale"
  )

  val isDark = isSystemInDarkTheme()
  val theme = remember(isDark) {
    win.buildTheme(isDark)
  };
  CompositionLocalProvider(
    LocalContentColor provides MaterialTheme.colorScheme.onPrimary,
    LocalWindowEdge provides winEdge,
    LocalWindowLimits provides limits,
    LocalWindowControllerTheme provides theme,
  ) {
    /// 窗口
    val aniDp = @Composable { value: Float, label: String ->
      (if (inMove or inResize) value else animateFloatAsState(value, label = label).value).dp
    }
    Box(
      modifier = with(winBounds) {
        modifier
          .offset(
            aniDp(left, "left"),
            aniDp(top, "top"),
          )
          .size(
            aniDp(width, "width"),
            aniDp(height, "height"),
          )
      }
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .shadow(
          elevation = elevation.dp, shape = winEdge.boxRounded.toRoundedCornerShape()
        ),
    ) {
      Column(Modifier
        .background(theme.winFrameBrush)
        .clip(winEdge.boxRounded.toRoundedCornerShape())
        .focusable(true)
        .onFocusChanged { state ->
          win.emitFocusOrBlur(state.isFocused)
        }
        .clickable {
          win.emitFocusOrBlur(true)
        }) {
        /// 标题栏
        WindowTopBar(win)
        /// 显示内容
        Box(
          Modifier
            .weight(1f)
            .padding(start = winEdge.left.dp, end = winEdge.right.dp)// TODO 这里要注意布局方向
        ) {
          val viewWidth = winEdge.contentBounds.width
          val viewHeight = winEdge.contentBounds.height
          /**
           * 视图的宽高随着窗口的缩小而缩小，随着窗口的放大而放大，
           * 但这些缩放不是等比的，而是会以一定比例进行换算。
           */
          windowAdapterManager.providers[win.state.wid]?.also {
            val viewScale = win.calcContentScale(limits, winEdge)
            it(
              modifier = Modifier
                .requiredSize(viewWidth.dp, viewHeight.dp)
                .clip(winEdge.contentRounded.toRoundedCornerShape()),
              width = viewWidth,
              height = viewHeight,
              scale = viewScale,
            )
          } ?: Text(
            "Op！视图被销毁了",
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.error,
            )
          )
        }
        /// 显示底部控制条
        WindowBottomBar(win)
      }

      /**
       * 窗口是否聚焦
       */
      val isFocus by win.state.watchedState { focus }

      /// 失去焦点的时候，提供 moveable 的遮罩（在移动中需要确保遮罩存在）
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
