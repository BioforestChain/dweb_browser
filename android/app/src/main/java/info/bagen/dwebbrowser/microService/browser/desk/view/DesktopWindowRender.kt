package info.bagen.dwebbrowser.microService.browser.desk.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import info.bagen.dwebbrowser.microService.browser.desk.DesktopWindowController
import info.bagen.dwebbrowser.microService.browser.desk.Float
import info.bagen.dwebbrowser.microService.browser.desk.toModifier
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt

@Composable
fun DesktopWindowController.Render(
  modifier: Modifier = Modifier,
  maxWinWidth: Float,
  maxWinHeight: Float
) {
  val win = this;
  var winState by remember { mutableStateOf(win.state, neverEqualPolicy()) }
  LaunchedEffect(win.state) {
    launch {
      winState.onChange.toFlow().collect {
        winState = win.state;
      }
    }
  }
  val coroutineScope = rememberCoroutineScope()
  val emitWinStateChange = { -> coroutineScope.launch { winState.emitChange() } }
  val emitWinFocus = { focused: Boolean ->
    coroutineScope.launch {
      if (focused) {
        focus()
      } else {
        blur()
      }
    }
  }
  val density = LocalDensity.current

  /**
   * 窗口是否在移动中
   */
  var inMove by remember { mutableStateOf(false) }
  fun Modifier.windowMoveAble() = this
    .pointerInput(Unit) {
      /// 触摸窗口的时候，聚焦，并且提示可以移动
      detectTapGestures(
        // touchStart 的时候，聚焦移动
        onPress = {
          inMove = true
          emitWinFocus(true)
        },
        /// touchEnd 的时候，取消移动
        onTap = {
          inMove = false
        }, onLongPress = {
          inMove = false
        })
    }
    .pointerInput(Unit) {
      /// 拖动窗口
      detectDragGestures(onDragStart = {
        inMove = true
        /// 开始移动的时候，同时进行聚焦
        emitWinFocus(true)
      }, onDragEnd = {
        inMove = false
      }, onDragCancel = {
        inMove = false
      }) { change, dragAmount ->
        change.consume()
        winState.bounds.left += dragAmount.x / density.density
        winState.bounds.top += dragAmount.y / density.density
        emitWinStateChange()
      }
    }

  /**
   * 窗口的最小缩放
   *
   * 和宽高不一样，缩放意味着保持宽高不变的情况下，将网页内容缩小，从而可以展示更多的网页内容
   */
  val minWinScale = 0.3;
  /**
   * 窗口最小宽度
   */
  val minWinWidth = maxWinWidth * 0.2f

  /**
   * 窗口最小高度
   */
  val minWinHeight = maxWinHeight * 0.2f

  /**
   * 窗口大小
   */
  val winBounds = if (winState.maximize) {
    inMove = false
    winState.bounds.copy(
      left = 0f,
      top = 0f,
      width = maxWinWidth,
      height = maxWinHeight,
    )
  } else {
    winState.bounds.copy(
      width = max(winState.bounds.width, minWinWidth),
      height = max(winState.bounds.height, minWinHeight)
    )
  };
  if (winBounds != winState.bounds) {
    winState.bounds = winBounds
    emitWinStateChange()
  }

  /**
   * 窗口内边距
   */
  val winEdge = if (winState.maximize) {
    val safeAreaPadding = WindowInsets.safeDrawing.asPaddingValues()
    val topHeight = safeAreaPadding.calculateTopPadding().value
    val bottomHeight = max(
      safeAreaPadding.calculateBottomPadding().value, 24f // 因为底部要放置一些信息按钮
    );
    val layoutDirection = LocalLayoutDirection.current
    val leftWidth = safeAreaPadding.calculateLeftPadding(layoutDirection).value
    val rightWidth = safeAreaPadding.calculateRightPadding(layoutDirection).value
    val borderRounded = 0f // 全屏模式下，外部不需要圆角
    val contentRounded = 16f // TODO 这里应该使用 WindowInsets#getRoundedCorner 来获得真实的无力圆角
    WindowEdge(topHeight, bottomHeight, leftWidth, rightWidth, borderRounded, contentRounded)
  } else {
    val borderRounded = 16f // TODO 这里应该使用 WindowInsets#getRoundedCorner 来获得真实的无力圆角
    val contentRounded = borderRounded / sqrt(2f)
    WindowEdge(36f, 24f, 5f, 5f, borderRounded, contentRounded)
  }


  val elevation by animateFloatAsState(
    targetValue = (if (inMove) 20f else 1f) + winState.zIndex,
    animationSpec = tween(durationMillis = if (inMove) 250 else 500),
    label = "elevation"
  )
  val scale by animateFloatAsState(
    targetValue = if (inMove) 1.05f else 1f,
    animationSpec = tween(durationMillis = if (inMove) 250 else 500),
    label = "scale"
  )

  CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
    /// 窗口
    Box(
      modifier = winBounds
        .toModifier(modifier)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .shadow(
          elevation = elevation.dp, shape = RoundedCornerShape(winEdge.boxRounded.dp)
        ),
    ) {
      Column(Modifier
        .background(MaterialTheme.colorScheme.onPrimaryContainer)
        .clip(RoundedCornerShape(winEdge.boxRounded.dp))
        .focusable(true)
        .onFocusChanged { state ->
          emitWinFocus(state.isFocused)
        }
        .clickable {
          emitWinFocus(true)
        }) {
        /// 标题栏
        WindowTopBar(
          Modifier.windowMoveAble(), winEdge, winState, win
        )
        /// 显示内容
        Box(Modifier.weight(1f)) {
          val viewHeight = winBounds.height - winEdge.top - winEdge.bottom
          val viewWidth = winBounds.width - winEdge.left - winEdge.right
          /**
           * 视图的宽高随着窗口的缩小而缩小，随着窗口的放大而放大，
           * 但这些缩放不是等比的，而是会以一定比例进行换算。
           */
          windowAdapterManager.providers[win.state.wid]?.also {
            /**
             * 计算进度
             */
            fun calcProgress(from: Float, now: Float, to: Float) =
              ((now - from) / (to - from)).toDouble()

            /**
             * 将动画进度还原成所需的缩放值
             */
            fun Double.toScale(minScale: Double, maxScale: Double = 1.0) =
              ((maxScale - minScale) * this) + minScale;

            val scaleProgress = max(
              calcProgress(minWinWidth, viewWidth, maxWinWidth),
              calcProgress(minWinHeight, viewHeight, maxWinHeight),
            )
            val viewScale =
              scaleProgress.toScale(minWinScale).let { if (it.isNaN()) 1f else it.toFloat() }

            //  println("minWidth, viewWidth, screenWidth: $minWidth, $viewWidth, $screenWidth")
            //  println("minHeight, viewHeight, screenHeight: $minHeight, $viewHeight, $screenHeight")
            //  println("scaleProgress: $scaleProgress")
            //  println("viewScale: $viewScale")

            it(
              Modifier
                .graphicsLayer(viewScale, viewScale)
                .requiredSize(
                  (viewWidth / viewScale).toInt().dp, (viewHeight / viewScale).toInt().dp
                )
                .clip(RoundedCornerShape(winEdge.contentRounded.dp))
            )
          } ?: Text(
            "Op！视图被销毁了",
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.headlineLarge.copy(
              color = MaterialTheme.colorScheme.error,
              background = MaterialTheme.colorScheme.errorContainer
            )
          )
        }
        /// 显示底部控制条
        WindowBottomBar(
          win,
          winEdge,
          winState,
          emitWinStateChange,
        )
      }

      /// 失去焦点的时候，提供 moveable 的遮罩（在移动中需要确保遮罩存在）
      if (inMove or !winState.focus) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (winState.focus) 0f else 0.2f))
            .windowMoveAble()
        )
      }
    }
  }
}

data class WindowEdge(
  val top: Float,
  val bottom: Float,
  val left: Float,
  val right: Float,
  /**
   * 外部圆角
   */
  val boxRounded: Float,
  /**
   * 内容圆角
   */
  val contentRounded: Float,
);