package org.dweb_browser.sys.window.render

import androidx.compose.animation.core.Animatable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
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
import org.dweb_browser.sys.window.core.renderConfig.WindowLayerStyle
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.helper.LocalWindowControllerTheme
import org.dweb_browser.sys.window.helper.LocalWindowFrameStyle
import org.dweb_browser.sys.window.helper.LocalWindowLimits
import org.dweb_browser.sys.window.helper.WindowFrameStyle
import org.dweb_browser.sys.window.helper.calcWindowContentScale
import org.dweb_browser.sys.window.helper.watchedBounds
import org.dweb_browser.sys.window.helper.watchedIsMaximized
import org.dweb_browser.sys.window.helper.watchedState
import org.dweb_browser.sys.window.helper.windowTouchFocusable

/**
 * 窗口渲染
 * 这一部分需要与 Prepare 分离
 * 否则在 desktop 中，这个 composeWindow 一旦停止渲染，上面那些就全部失效了，所以上面那些需要放在主窗口中去执行
 *
 * 除非说 window.isVisible 是走全透明方案……，但这样做会很奇怪，违反原生窗口的生命周期行为
 */
@Composable
fun WindowController.WindowRender(modifier: Modifier = Modifier) {
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
  val winFrameStyle = LocalWindowFrameStyle.current

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
  LaunchedEffect(isVisible, inMove) {
    launch {
      val scaleTo = when {
        isVisible -> when {
          inMove -> 1.05f
          else -> 1f
        }

        else -> 0.38f
      }
      scaleAni.animateTo(
        scaleTo,
        iosTween(durationIn = scaleTo > scaleAni.value)
      )
    }
    launch {
      val opacityTo = when {
        isVisible -> 1f
        else -> 0f
      }
      opacityAni.animateTo(opacityTo, iosTween(durationIn = opacityTo > opacityAni.value))
    }
  }
  val scale = scaleAni.value
  val opacity = opacityAni.value
  if (opacity == 0f) {
    return
  }

  LocalCompositionChain.current.Provider(
    LocalFocusRequester provides win.focusRequester,
    LocalWindowMM provides (win.state.constants.microModule.value ?: LocalWindowMM.current),
  ) {
    /// 开始绘制窗口
    win.state.safePadding = winFrameStyle.frameSafeAreaInsets
    val inResizeFrame by win.inResize
    val winBounds by win.watchedBounds()
    var inResizeAnimation by remember { mutableStateOf(false) }
    val windowRectNoTranslate = inResizeFrame || inMove

    /**
     * 窗口海拔，决定阴影效果
     */
    val windowElevation = animateFloatAsState(
      targetValue = (if (inMove) 20f else 1f) + zIndex,
      animationSpec = tween(durationMillis = if (inMove) 250 else 500),
      label = "elevation"
    ).value.dp

    /**
     * 窗口的形状描述，决定圆角效果
     */
    val windowCornerRadius = when {
      windowRectNoTranslate -> winFrameStyle.frameRounded// roundedCornerShape
      else -> winFrameStyle.frameRounded.run {
        val aniSpec = iosTween<Float>(durationIn = isMaximized)
        WindowFrameStyle.CornerRadius(
          topStart = animateFloatAsState(topStart, aniSpec).value,
          topEnd = animateFloatAsState(topEnd, aniSpec).value,
          bottomStart = animateFloatAsState(bottomStart, aniSpec).value,
          bottomEnd = animateFloatAsState(bottomEnd, aniSpec).value,
        )
      }
    }

    Box(
      modifier = when {
        // 如果使用 原生窗口的边框，那么只需要填充满画布即可
        win.state.renderConfig.isSystemWindow -> {
          win.state.renderConfig.effectWindowLayerStyleDelegate?.effectStyle(
            WindowLayerStyle(scale, opacity, windowElevation, windowCornerRadius)
          )
          modifier.fillMaxSize()
        }
        // 否则使用 模拟窗口的边框，需要自定义坐标、阴影、缩放
        else -> {
          val windowRect = winBounds.toRect().let { rect ->
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
          modifier.offset(windowRect.left.dp, windowRect.top.dp).size(
            windowRect.width.dp, windowRect.height.dp
          ).graphicsLayer {
            this.alpha = opacity
            this.scaleX = scale
            this.scaleY = scale
          }.shadow(
            elevation = windowElevation,
            shape = windowCornerRadius.roundedCornerShape,
          ).focusable()
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

        val topBarHeight = winFrameStyle.frameSize.top
        val bottomBarHeight = winFrameStyle.frameSize.bottom
        val virtualNavigationBarHeight = getVirtualNavigationBarHeight()
        /// 标题栏
        WindowTopBar(
          win,
          Modifier.height(topBarHeight.dp).fillMaxWidth()
        )
        /// 内容区域
        BoxWithConstraints(
          Modifier.weight(1f).padding(start = 0.dp, end = 0.dp)
            .clip(winFrameStyle.contentRounded.roundedCornerShape)
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
              scale = calcWindowContentScale(limits, targetWidth.value, targetHeight.value),
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
        WindowBottomBar(
          win,
          Modifier.height(bottomBarHeight.dp).padding(
            bottom = when (isMaximized) {
              true -> virtualNavigationBarHeight.dp
              else -> 0.dp
            }
          )
            .fillMaxWidth()
        )
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
