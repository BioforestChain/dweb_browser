package org.dweb_browser.sys.window.helper

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import org.dweb_browser.helper.PureBounds
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.getCornerRadiusBottom
import org.dweb_browser.helper.platform.getCornerRadiusTop
import org.dweb_browser.helper.platform.isAndroid
import org.dweb_browser.helper.platform.isIOS
import org.dweb_browser.helper.platform.rememberPureViewBox
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowState
import org.dweb_browser.sys.window.core.constant.WindowBottomBarTheme
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys
import org.dweb_browser.sys.window.render.getVirtualNavigationBarHeight
import org.dweb_browser.sys.window.render.inMove
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 根据约束配置，计算出最终的窗口边框的样式信息
 */
@Composable
fun WindowController.calcWindowFrameStyle(
  limits: WindowLimits,
): WindowFrameStyle {
  /**
   * 窗口大小
   */
  val winBounds = calcWindowBoundsByLimits(this, limits)
  /**
   * 窗口边距
   */
  return calcWindowFrameStyle(this, limits, winBounds)
}

/**
 * 根据约束配置，计算出最终的窗口大小与坐标
 */
@Composable
private fun calcWindowBoundsByLimits(
  win: WindowController,
  limits: WindowLimits,
): PureRect {
  return if (win.watchedIsMaximized().value) {
    win.inMove.value = false
    // 原生窗口在最大化后，bounds已经由外部进行了修改
    if (win.state.updateBoundsReason == WindowState.UpdateReason.Outer) {
      win.watchedBounds().value
    } else {
      // TODO 如果进行最大化，simpleMaximized 函数需要自己处理 这个updateBounds 才对，而不是到这里 compose 函数中来修改
      win.state.updateBounds {
        copy(
          x = 0f,
          y = 0f,
          width = limits.maxWidth,
          height = limits.maxHeight,
        )
      }
    }
  } else {
    // 这里不要用 watchedBounds，会导致冗余的计算循环
    val bounds = win.state.bounds

    /**
     * 获取可触摸的空间
     */
    val winHeight = max(bounds.height, limits.minHeight)
    val winWidth = max(bounds.width, limits.minWidth)
    val padding = win.safeBounds(limits)
    win.state.updateBounds {
      copy(
        x = min(max(padding.left, bounds.x), padding.right),
        y = min(max(padding.top, bounds.y), padding.bottom),
        width = winWidth,
        height = winHeight,
      )
    }
  }
}

/**
 * 根据约束配置，计算出最终的窗口边距布局
 */
@Composable
private fun calcWindowFrameStyle(
  win: WindowController,
  limits: WindowLimits,
  bounds: PureRect,// 这里这个不要通过 watchBounds 获得，这会有延迟，应该是直接通过传递参数获得
): WindowFrameStyle {
  val maximize by win.watchedIsMaximized()
  val bottomBarTheme by win.watchedState(watchKey = WindowPropertyKeys.BottomBarTheme) { bottomBarTheme }
  val topHeight: Float
  val bottomHeight: Float
  val leftWidth: Float
  val rightWidth: Float
  val borderRounded: WindowFrameStyle.CornerRadius
  val contentRounded: WindowFrameStyle.CornerRadius
  val boxSafeAreaInsets: PureBounds
  val contentSafeAreaInsets: PureBounds
  /// 一些共有的计算
  val windowFrameSize = if (maximize) 2f else 3f

  /**
   * safeGestures = systemGestures + mandatorySystemGestures + waterfall + tappableElement.
   */
  val safeGestures = WindowInsets.safeGestures

  /**
   * safeDrawing = systemBars + displayCutout + ime
   */
  val safeDrawing = WindowInsets.safeDrawing
  val density = LocalDensity.current
  val d = LocalDensity.current.density

  val safeGesturesBottom = safeGestures.getBottom(density) / d
  val virtualNavigationBarheight = getVirtualNavigationBarHeight()

  /**
   * 不同的底部栏风格有不同的高度
   */
  val bottomThemeHeight = when (bottomBarTheme) {
    WindowBottomBarTheme.Immersion -> limits.bottomBarBaseHeight// 因为底部要放置一些信息按钮，所以我们会给到底部一个基本的高度
    WindowBottomBarTheme.Navigation -> {
      /* 要有足够的高度放按钮和基本信息 */
      val baseHeight = 32f
      max(
        limits.bottomBarBaseHeight, when {
          // 如果最大化，那么要考虑操作系统的导航栏高度
          maximize -> when {
            IPureViewController.isAndroid -> when {
              /**
               * 如果用户使用了按钮导航栏，那么直接使用按钮导航栏的高度作为我们底部导航栏的高度
               */
              safeGesturesBottom >= 40f -> safeGesturesBottom

              /**
               * 如果用户使用了全面屏手势，那么尝试将 baseHeight 加上 导航栏的高度作为我们的底部导航栏高度
               */
              /**
               * 如果用户使用了全面屏手势，那么尝试将 baseHeight 加上 导航栏的高度作为我们的底部导航栏高度
               */
              else -> min(48f, safeGesturesBottom + baseHeight)
            } + virtualNavigationBarheight
//            IPureViewController.isAndroid -> safeGesturesBottom + baseHeight

            IPureViewController.isIOS -> when {
              /// IOS 的全屏导航栏高度是 34f，这里的取值是确保 基本信息是在导航条的下方，按钮在导航条的上方
              safeGesturesBottom > 0f -> 48f
              else -> baseHeight
            }

            else -> baseHeight
          }

          else -> baseHeight
        }
      )
    }
  }

  if (maximize) {
    val layoutDirection = LocalLayoutDirection.current

//    /**
//     *  safeContent = safeDrawing + safeGestures
//     */
//    val safeContentPadding = WindowInsets.safeContent.asPaddingValues()
//    val safeContentPaddingBottom = safeContentPadding.calculateBottomPadding().value
    val safeDrawingPaddingTop = safeDrawing.getTop(density) / d
    val safeGesturesPaddingBottom = safeGestures.getBottom(density) / d
    // 顶部的高度，可以理解为状态栏的高度
    topHeight = max(safeDrawingPaddingTop, windowFrameSize)
    /**
     * 底部是系统导航栏，这里我们使用触摸安全的区域来控制底部高度，这样可以避免底部抖动
     * 不该使用 safeDrawing，它会包含 ime 的高度
     */
    bottomHeight = max(max(bottomThemeHeight, safeGesturesPaddingBottom), windowFrameSize)
    /**
     * 即便是最大化模式下，我们仍然需要有一个强调边框。
     * 这个边框存在的意义有：
     * 1. 强调是窗口模式，而不是全屏模式
     * 2. 养成用户的视觉习惯，避免某些情况下有人使用视觉手段欺骗用户，窗口模式的存在将一切限定在一个规则内，可以避免常规视觉诈骗
     * 3. 全屏模式虽然会移除窗口，但是会有一些其它限制，比如但需要进行多窗口交互的时候，这些窗口边框仍然会显示出来
     */
    leftWidth = max(safeDrawing.getLeft(density, layoutDirection) / d, windowFrameSize)
    rightWidth = max(safeDrawing.getRight(density, layoutDirection) / d, windowFrameSize)
    borderRounded = getWindowControllerBorderRounded(true) // 全屏模式下，外部不需要圆角
    val platformViewController = rememberPureViewBox()
    contentRounded = WindowFrameStyle.CornerRadius.from(
      getCornerRadiusTop(platformViewController, d, 16f),
      getCornerRadiusBottom(platformViewController, d, 16f)
    )

    boxSafeAreaInsets =
      PureBounds.Zero.copy(bottom = max(safeGesturesPaddingBottom - bottomHeight, 0f))
  } else {
    borderRounded = getWindowControllerBorderRounded(false)
    contentRounded = borderRounded / sqrt(2f)
    topHeight = max(limits.topBarBaseHeight, windowFrameSize)
    bottomHeight = max(bottomThemeHeight, windowFrameSize)
    leftWidth = windowFrameSize
    rightWidth = windowFrameSize

    boxSafeAreaInsets = PureBounds.Zero
  }
  return WindowFrameStyle(
    frameSize = PureBounds(
      top = topHeight,
      bottom = bottomHeight,
      left = leftWidth,
      right = rightWidth,
    ),
    frameRounded = borderRounded,
    contentRounded = contentRounded,
    frameSafeAreaInsets = boxSafeAreaInsets,
  )
}