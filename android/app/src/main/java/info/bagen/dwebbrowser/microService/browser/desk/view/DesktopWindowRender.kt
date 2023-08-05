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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.base.WindowInsetsHelper
import info.bagen.dwebbrowser.microService.browser.desk.DesktopWindowController
import info.bagen.dwebbrowser.microService.browser.desk.Float
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun DesktopWindowController.Render(
  modifier: Modifier = Modifier, maxWinWidth: Float, maxWinHeight: Float
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
  val layoutDirection = LocalLayoutDirection.current

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
   * 窗口模式下的窗口标题高度
   */
  val winTitleBaseHeight = 36f;

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
    val safeGesturesPadding = WindowInsets.safeGestures.asPaddingValues();
    val width = max(winState.bounds.width, minWinWidth)
    val height = max(winState.bounds.height, minWinHeight)
    val safeLeftPadding = safeGesturesPadding.calculateLeftPadding(layoutDirection).value;
    val safeTopPadding = safeGesturesPadding.calculateTopPadding().value;
    val safeRightPadding = safeGesturesPadding.calculateRightPadding(layoutDirection).value;
    val safeBottomPadding = safeGesturesPadding.calculateBottomPadding().value;
    val minLeft = safeLeftPadding - width / 2
    val maxLeft = maxWinWidth - safeRightPadding - width / 2
    val minTop = safeTopPadding
    val maxTop = maxWinHeight - safeBottomPadding - winTitleBaseHeight
    winState.bounds.copy(
      left = min(max(minLeft, winState.bounds.left), maxLeft),
      top = min(max(minTop, winState.bounds.top), maxTop),
      width = width,
      height = height,
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
    val safeContentPadding = WindowInsets.safeContent.asPaddingValues()
    val safeGesturesPadding = WindowInsets.safeGestures.asPaddingValues()
    val topHeight = safeContentPadding.calculateTopPadding().value

    /**
     * 底部是系统导航栏，这里我们使用触摸安全的区域来控制底部高度，这样可以避免底部抖动
     */
    val bottomMinHeight = max(
      safeGesturesPadding.calculateBottomPadding().value, 24f // 因为底部要放置一些信息按钮，所以我们会给到底部一个基本的高度
    )
    val bottomHeight = max(
      safeContentPadding.calculateBottomPadding().value, bottomMinHeight
    );
    /**
     * 即便是最大化模式下，我们仍然需要有一个强调边框。
     * 这个边框存在的意义有：
     * 1. 强调是窗口模式，而不是全屏模式
     * 2. 养成用户的视觉习惯，避免某些情况下有人使用视觉手段欺骗用户，窗口模式的存在将一切限定在一个规则内，可以避免常规视觉诈骗
     * 3. 全屏模式虽然会移除窗口，但是会有一些其它限制，比如但需要进行多窗口交互的时候，这些窗口边框仍然会显示出来
     */
    val leftWidth = max(safeContentPadding.calculateLeftPadding(layoutDirection).value, 3f)
    val rightWidth = max(safeContentPadding.calculateRightPadding(layoutDirection).value, 3f)
    val borderRounded = WindowEdge.CornerRadius.from(0) // 全屏模式下，外部不需要圆角
    val contentRounded = WindowEdge.CornerRadius.from(
      WindowInsetsHelper.getCornerRadiusTop(win.context, density.density, 16f),
      WindowInsetsHelper.getCornerRadiusBottom(win.context, density.density, 16f)
    )
    val contentSize = WindowEdge.ContentSize(
      winBounds.width - leftWidth - rightWidth,
      winBounds.height - topHeight - bottomMinHeight, // 这里不使用bottomHeight，因为导航栏的高度会发生动态变动，因此使用bottomMinHeight可以有效避免抖动
    )
    WindowEdge(
      topHeight,
      bottomHeight,
      leftWidth,
      rightWidth,
      borderRounded,
      contentRounded,
      contentSize
    )
  } else {
    val borderRounded =
      WindowEdge.CornerRadius.from(16) // TODO 这里应该使用 WindowInsets#getRoundedCorner 来获得真实的无力圆角
    val contentRounded = borderRounded / sqrt(2f)
    val topHeight = winTitleBaseHeight;
    val bottomHeight = 24f
    val leftWidth = 5f;
    val rightWidth = 5f;
    val contentSize = WindowEdge.ContentSize(
      winBounds.width - leftWidth - rightWidth,
      winBounds.height - topHeight - bottomHeight, // 这里不使用bottomHeight，因为导航栏的高度会发生动态变动
    )
    WindowEdge(36f, 24f, 5f, 5f, borderRounded, contentRounded, contentSize)
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
      modifier = with(winBounds) {
        modifier
          .offset(
            (if (inMove) left else animateFloatAsState(left, label = "left").value).dp,
            (if (inMove) top else animateFloatAsState(top, label = "top").value).dp
          )
          .size(
            (if (inMove) width else animateFloatAsState(width, label = "width").value).dp,
            (if (inMove) height else animateFloatAsState(height, label = "height").value).dp
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
      /**
       * 窗口的主色调
       */
      val windowFrameColor =
        if (winState.focus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
      val windowContentColor =
        if (winState.focus) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary

      Column(Modifier
        .background(windowFrameColor)
        .clip(winEdge.boxRounded.toRoundedCornerShape())
        .focusable(true)
        .onFocusChanged { state ->
          emitWinFocus(state.isFocused)
        }
        .clickable {
          emitWinFocus(true)
        }) {
        /// 标题栏
        WindowTopBar(
          Modifier.windowMoveAble(), winEdge, winState, win, windowContentColor
        )
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
          windowContentColor,
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
  val boxRounded: CornerRadius,
  /**
   * 内容圆角
   */
  val contentRounded: CornerRadius,
  val contentBounds: ContentSize
) {
  data class CornerRadius(
    val topStart: Float,
    val topEnd: Float,
    val bottomStart: Float,
    val bottomEnd: Float
  ) {
    operator fun div(value: Float) =
      CornerRadius(topStart / value, topEnd / value, bottomStart / value, bottomEnd / value)

    fun toRoundedCornerShape() =
      RoundedCornerShape(topStart.dp, topEnd.dp, bottomStart.dp, bottomEnd.dp)

    companion object {
      fun from(radius: Float) = CornerRadius(radius, radius, radius, radius)
      fun from(radius: Int) = from(radius.toFloat())
      fun from(topRadius: Float, bottomRadius: Float) =
        CornerRadius(topRadius, topRadius, bottomRadius, bottomRadius)

      fun from(topRadius: Int, bottomRadius: Int) =
        from(topRadius.toFloat(), bottomRadius.toFloat())
    }
  }

  data class ContentSize(val width: Float, val height: Float)
};