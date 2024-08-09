package org.dweb_browser.sys.window.render

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.LocalFocusRequester
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.iosTween
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.bindPureViewController
import org.dweb_browser.helper.platform.unbindPureViewController
import org.dweb_browser.helper.toRect
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.LocalWindowMM
import org.dweb_browser.sys.window.core.windowAdapterManager

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
  val winPadding = win.calcWindowByLimits(limits)

  val theme = win.buildTheme();
  CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
    LocalCompositionChain.current.Provider(
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

      val isVisible by win.watchedState { isVisible }

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

/**
 * 窗口渲染
 * 这一部分需要与 Prepare 分离
 * 否则在 desktop 中，这个 composeWindow 一旦停止渲染，上面那些就全部失效了，所以上面那些需要放在主窗口中去执行
 *
 * 除非说 window.isVisible 是走全透明方案……，但这样做会很奇怪，违反原生窗口的生命周期行为
 */
@Composable
fun WindowController.WindowRender(modifier: Modifier) {
  val win = this

  win.state.constants.microModule.value?.also { microModule ->
    val pureViewController = LocalPureViewController.current
    DisposableEffect(microModule, pureViewController) {
      microModule.bindPureViewController(pureViewController)
      onDispose {
        microModule.unbindPureViewController()
      }
    }
  }

  val isVisible by win.watchedState { win.isVisible }
  val inMove by win.inMove

  /**
   * 窗口边距
   */
  val winPadding = LocalWindowPadding.current

  /**
   * 窗口层级
   */
  val zIndex by win.watchedState { zIndex }

  /**
   * 窗口缩放
   *
   * 目前，它只适配了 拖动窗口时将窗口放大的动画效果
   * TODO 需要监听 winBounds 的 height/width 变化，将这个变化适配到窗口的 scaleX、scaleY 上，只有在动画完成的时候，才会正式将真正的 size 传递给内容渲染，这样可以有效避免内容频繁的resize渲染计算。这种resize有两种情况，一种是基于用户行为的resize、一种是基于接口行为的（比如最大化），所以应该统一通过监听winBounds变更来动态生成scale动画
   */
  val scaleAni = remember { Animatable(0.38f) }
  val opacityAni = remember { Animatable(0f) }
  LaunchedEffect(isVisible || inMove) {
    launch {
      scaleAni.animateTo(
        if (inMove) 1.05f else if (isVisible) 1f else 0.38f,
        iosTween(durationIn = isVisible)
      )
    }
    launch {
      opacityAni.animateTo(if (isVisible) 1f else 0f, iosTween(durationIn = isVisible))
    }
  }
  val scale = scaleAni.value
  val opacity = opacityAni.value
  if (opacity == 0f) {
    return
  }

  val windowFrameStyle = WindowFrameStyle(scale, opacity)
  val windowContentStyle = WindowContentStyle(windowFrameStyle)
  LocalCompositionChain.current.Provider(
    LocalWindowFrameStyle provides windowFrameStyle,
    LocalWindowContentStyle provides windowContentStyle,
    LocalFocusRequester provides win.focusRequester,
    LocalWindowMM provides (win.state.constants.microModule.value ?: LocalWindowMM.current),
  ) {
    /// 开始绘制窗口
    win.state.safePadding = winPadding.boxSafeAreaInsets
    val inResizeFrame by win.inResize
    val winBounds by win.watchedBounds()
    var inResizeAnimation by remember { mutableStateOf(false) }
    val windowRectNoTranslate = inResizeFrame || inMove
    val windowRect = when {
      win.state.renderConfig.isSystemWindow -> null
      else -> winBounds.toRect().let { rect ->
        when {
          windowRectNoTranslate -> rect
          else -> animateRectAsState(
            targetValue = rect,
            animationSpec = iosTween(durationIn = isMaximized),
            label = "bounds-rect",
          ).value.also {
            inResizeAnimation = it != rect
          }
        }
      }
    }
    Box(
      modifier = modifier.composed {
        when (windowRect) {
          // 如果使用 原生窗口的边框，那么只需要填充满画布即可
          null -> fillMaxSize()
          // 否则使用 模拟窗口的边框，需要自定义坐标、阴影、缩放
          else -> {
            offset(windowRect.left.dp, windowRect.top.dp).size(
              windowRect.width.dp, windowRect.height.dp
            ).graphicsLayer {
              this.alpha = opacity
              this.scaleX = scale
              this.scaleY = scale
            }.shadow(/**
             * 窗口海拔阴影
             */
              elevation = animateFloatAsState(
                targetValue = (if (inMove) 20f else 1f) + zIndex,
                animationSpec = tween(durationMillis = if (inMove) 250 else 500),
                label = "elevation"
              ).value.dp, shape = winPadding.boxRounded.run {
                when {
                  windowRectNoTranslate -> roundedCornerShape
                  else -> RoundedCornerShape(
                    animateDpAsState(topStart.dp, iosTween(durationIn = isMaximized)).value,
                    animateDpAsState(topEnd.dp, iosTween(durationIn = isMaximized)).value,
                    animateDpAsState(bottomStart.dp, iosTween(durationIn = isMaximized)).value,
                    animateDpAsState(bottomEnd.dp, iosTween(durationIn = isMaximized)).value,
                  )
                }
              }).focusable()
          }
        }
      },
    ) {
      val theme = LocalWindowControllerTheme.current
      //#region 窗口内容
      Column(Modifier.background(theme.winFrameBrush).clickableWithNoEffect {
        win.focusInBackground()
      }) {
        val density = LocalDensity.current.density
        fun safeDp(dp: Dp): Dp {
          val dpValue = (dp.value * density).toInt() / density
          return when (dp.value) {
            dpValue -> dp
            else -> dpValue.dp
          }
        }

        var topBarHeight by remember { mutableFloatStateOf(Float.NaN) }
        var bottomBarHeight by remember { mutableFloatStateOf(Float.NaN) }
        /// 标题栏
        WindowTopBar(win, Modifier.height(winPadding.top.dp).fillMaxWidth().onGloballyPositioned {
          topBarHeight = it.size.height / density
        })
        /// 内容区域
        BoxWithConstraints(
          Modifier.weight(1f).padding(start = 0.dp, end = 0.dp)
            .clip(winPadding.contentRounded.roundedCornerShape)
        ) {
          /// 底部安全区域
          val keyboardInsetBottom by win.watchedState { keyboardInsetBottom }
          val keyboardOverlaysContent by win.watchedState { keyboardOverlaysContent }

          /**
           * 用于扣除的底部区域
           * 这边使用键盘与窗口的交集作为底部区域
           */
          val paddingBottom = if (keyboardOverlaysContent) 0f else keyboardInsetBottom

          val limits = LocalWindowLimits.current
          val isResizing = inResizeAnimation || inResizeFrame
          val windowRenderScope = remember(
            limits,
            maxWidth,
            maxHeight,
            winBounds,
            isResizing,
            paddingBottom,
            topBarHeight,
            bottomBarHeight
          ) {
            val targetHeight: Dp
            val targetWidth: Dp
            if (!isResizing || topBarHeight.isNaN() || bottomBarHeight.isNaN()) {
              targetWidth = maxWidth
              targetHeight = maxHeight - paddingBottom.dp
            } else {
              targetWidth = safeDp(winBounds.width.dp)
              targetHeight =
                safeDp((winBounds.height - topBarHeight - paddingBottom - bottomBarHeight).dp)
            }
            WindowContentRenderScope(
              widthDp = targetWidth,
              heightDp = targetHeight,
              scale = win.calcContentScale(limits, targetWidth.value, targetHeight.value),
              isResizing = isResizing
            )
          }
          /// 显示内容
          Column {
            /// 使用 flex 布局，确保 viewPort 稳定
            Box(Modifier.weight(1f)) {
              windowAdapterManager.Renderer(
                win.state.constants.wid, windowRenderScope, Modifier.fillMaxSize()
              )
            }
            Box(Modifier.fillMaxWidth().height(paddingBottom.dp))
          }
        }
        /// 显示底部控制条
        WindowBottomBar(win,
          Modifier.height(winPadding.bottom.dp).fillMaxWidth().onGloballyPositioned {
            bottomBarHeight = it.size.height / density
          })
      }
      //#endregion

      /**
       * 窗口是否聚焦
       */
      val isFocus by win.watchedState { focus }

      /// 失去焦点的时候，提供 movable 的遮罩（在移动中需要确保遮罩存在）
      if (inMove or !isFocus) {
        Box(modifier = Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (isFocus) 0f else 0.2f))
          .run {
            val isMaximized by win.watchedIsMaximized()
            /// 如果最大化，那么不允许移动
            when {
              isMaximized -> windowTouchFocusable(win)
              else -> windowMoveAble(win)
            }
          })
      }
    }
  }
}
