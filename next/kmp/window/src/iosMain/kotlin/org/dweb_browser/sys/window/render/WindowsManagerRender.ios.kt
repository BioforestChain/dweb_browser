package org.dweb_browser.sys.window.render

import androidx.compose.animation.core.animateRectAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.compose.CompositionChain
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.iosTween
import org.dweb_browser.helper.compose.toOffset
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.FixIosLayerRefresh
import org.dweb_browser.helper.platform.FixRefreshMode
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.rememberInterfaceOrientation
import org.dweb_browser.helper.toPureRect
import org.dweb_browser.helper.toRect
import org.dweb_browser.sys.window.core.LocalWindowsManager
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.constant.debugWindow
import org.dweb_browser.sys.window.core.renderConfig.EffectWindowLayerStyleDelegate
import org.dweb_browser.sys.window.core.renderConfig.FrameDragDelegate
import org.dweb_browser.sys.window.helper.LocalWindowLimits
import org.dweb_browser.sys.window.helper.safeBounds
import org.dweb_browser.sys.window.helper.watchedBounds
import org.dweb_browser.sys.window.helper.watchedIsMaximized
import org.dweb_browser.sys.window.helper.watchedState


@Composable
actual fun <T : WindowController> WindowsManager<T>.SceneRender(modifier: Modifier) {
  val windowsManager = this
  BoxWithConstraints(modifier) {
    WindowsManagerEffect()
    /// 普通层级的窗口
    val winList by winListFlow.collectAsState()
    debugWindow("WindowsManager.Render", "winList: ${winList.size}")
    for (win in winList) {
      key(win.id) {
        IosWindowPrepare(windowsManager, win, maxWidth.value, maxHeight.value, 10)
      }
    }
    /// 置顶层级的窗口
    val topWinList by topWinListFlow.collectAsState()
    debugWindow("WindowsManager.Render", "winListTop: ${topWinList.size}")
    for (win in topWinList) {
      key(win.id) {
        IosWindowPrepare(windowsManager, win, maxWidth.value, maxHeight.value, 10000)
      }
    }
  }
}

private class IosWindowNativeView(
  private val win: WindowController,
  private val windowsManager: WindowsManager<*>,
  val compositionChainState: MutableState<CompositionChain>,
) {
  val pvc = PureViewController(fullscreen = false).also { pvc ->
    win.onClose {
      nativeViewController.remove(pvc)
    }
    pvc.onCreate {
      @OptIn(InternalComposeUiApi::class)
      pvc.addContent {
        /// 监听屏幕旋转，修复旋转屏幕后 pvc 内的compose 没有进行重新渲染导致的大问题
        val orientation = rememberInterfaceOrientation()
        FixIosLayerRefresh(remember(orientation) {
          FixRefreshMode.Companion.FixRefreshTimerMode(
            id = datetimeNow(),
            time = 100,//16
            delay = 220,//268
          )
        })

        val compositionChain by compositionChainState
        (compositionChain + LocalCompositionChain.current).Provider(LocalWindowsManager provides windowsManager) {
          /// 渲染窗口
          win.WithMaterialTheme {
            win.WindowRender(Modifier)
          }
        }
      }
    }
  }

  companion object {
    private val INSTANCES = WeakHashMap<WindowController, IosWindowNativeView>()

    @Composable
    fun from(
      win: WindowController,
      windowsManager: WindowsManager<*>,
      createCompositionChainState: @Composable () -> MutableState<CompositionChain>,
    ) = IosWindowNativeView.INSTANCES.getOrPut(win) {
      IosWindowNativeView(
        win = win,
        windowsManager = windowsManager,
        compositionChainState = createCompositionChainState(),
      )
    }
  }
}

@Composable
private fun IosWindowPrepare(
  windowsManager: WindowsManager<*>,
  win: WindowController,
  sceneMaxWidth: Float,
  sceneMaxHeight: Float,
  zIndexBase: Int,
) {
  win.Prepare(
    winMaxWidth = sceneMaxWidth,
    winMaxHeight = sceneMaxHeight,
  ) {
    val iosWindowNativeView = IosWindowNativeView.from(
      win = win,
      windowsManager = windowsManager,
      createCompositionChainState = { mutableStateOf(LocalCompositionChain.current) }
    )
    iosWindowNativeView.compositionChainState.value = LocalCompositionChain.current
    val pvc = iosWindowNativeView.pvc
    val zIndex by win.watchedState(zIndexBase) { zIndex + zIndexBase }
    /// 视图的显示与关闭
    DisposableEffect(pvc) {
      pvc.lifecycleScope.launch {
        nativeViewController.addOrUpdate(pvc, visible = true)
      }
      onDispose {
        pvc.lifecycleScope.launch {
          if (pvc.isAdded) {// 可能已经被销毁了
            nativeViewController.addOrUpdate(pvc, visible = false)
          }
        }
      }
    }
    /// 切换zIndex
    LaunchedEffect(zIndex) {
      nativeViewController.addOrUpdate(pvc, zIndex)
    }
    /// 视图的一些初始化配置
    LaunchedEffect(pvc) {
      // 我们使用原生的窗口图层
      win.state.renderConfig.isSystemWindow = true
      // 但仍然使用compose进行窗口边框绘制
      win.state.renderConfig.isWindowUseComposeFrame = true
      /// 应用窗口样式
      val vc = pvc.uiViewControllerInMain
      win.state.renderConfig.effectWindowLayerStyleDelegate = EffectWindowLayerStyleDelegate {
        vc.view.effectWindowFrameStyle(it)
      }
    }
    /// 键盘视图的交叉渲染
    val windowImeOutsetBounds = calcWindowImeOutsetBounds(windowsManager, win)
    win.state.keyboardInsetBottom = windowImeOutsetBounds.keyboardInsetBottom

    /// 绑定窗口的坐标与大小
    val inMove by win.inMove
    val inResizeFrame by win.inResize

    // 用户手势操作窗口大小时，由操作器自己更新bounds，否则会有延迟。
    if (!inMove && !inResizeFrame) {
      // 同时这里使用动画方案来进行bounds的更新。以确保和android平台保持相似的体验
      AnimationWindowBoundsEffect(pvc, win, windowImeOutsetBounds.modifierOffsetY)
    }
    /// 响应窗口的move和resize行为
    val limits = LocalWindowLimits.current
    val safeBounds = win.safeBounds(limits)
    val density = LocalDensity.current.density
    LaunchedEffect(win, limits, density) {
      val rootView = pvc.uiViewControllerInMain.view
      fun buildPvcDragDelegate(onMove: (dragAmount: Offset) -> Unit): FrameDragDelegate {
        var prevPosition = Offset.Zero
        var prevViewPosition = Offset.Zero
        return FrameDragDelegate(
          onStart = { pointerPositionPx ->
            val viewPosition = pvc.getPosition().toOffset()
            val pointerPosition = pointerPositionPx / density
            val screenPosition = pointerPosition + viewPosition
            prevPosition = screenPosition
            prevViewPosition = screenPosition
          },
          onMove = { change, changeAmount ->
            val viewPosition = pvc.getPosition().toOffset()
            val pointerPosition = change.position / density
            val screenPosition = pointerPosition + viewPosition
            // 奇葩的IOS，如果 viewPosition 没有变化，那么这个 change.position 居然会抖动错乱，但是 changeAmount 反而是正常的
            // 很类似如果是 viewPosition 没有变化的时候，change.position 的计算会错误，同时 prevPosition 延续了这个错误的计算，所以 changeAmount 居然神奇地对了
            val moveX: Float = when (prevViewPosition.x) {
              viewPosition.x -> changeAmount.x / density
              else -> screenPosition.x - prevPosition.x
            }
            val moveY: Float = when (prevViewPosition.y) {
              viewPosition.y -> changeAmount.y / density
              else -> screenPosition.y - prevPosition.y
            }

            val dragAmount = Offset(x = moveX, y = moveY)// screenPosition - prevPosition
            prevPosition = screenPosition
            prevViewPosition = viewPosition
            onMove(dragAmount)
          },
        )
      }
      win.state.renderConfig.apply {
        /// move
        frameMoveDelegate = buildPvcDragDelegate { dragAmount ->
          val movingWinBounds = win.state.updateBounds {
            moveWindowBoundsInSafeBounds(this, safeBounds, dragAmount)
          }
          pvc.setBoundsInMain(movingWinBounds, rootView)
        }
        /// resize left-bottom
        frameLBResizeDelegate = buildPvcDragDelegate { dragAmount ->
          val resizingWinBounds = win.state.updateBounds {
            resizeWindowBoundsInLeftBottom(this, limits, dragAmount)
          }
          pvc.setBoundsInMain(resizingWinBounds, rootView)
        }

        /// resize right-bottom
        frameRBResizeDelegate = buildPvcDragDelegate { dragAmount ->
          val resizingWinBounds = win.state.updateBounds {
            resizeWindowBoundsInRightBottom(this, limits, dragAmount)
          }
          pvc.setBoundsInMain(resizingWinBounds, rootView)
        }
      }
    }
  }
}

@Composable
private fun AnimationWindowBoundsEffect(
  pvc: PureViewController,
  win: WindowController,
  boundsOffsetY: Float,
) {
  val isMaximized by win.watchedIsMaximized()
  val winBounds by win.watchedBounds()
  val winAniBounds by animateRectAsState(
    targetValue = when (boundsOffsetY) {
      0f -> winBounds
      else -> winBounds.mutable {
        y += boundsOffsetY
      }
    }.toRect(),
    animationSpec = iosTween(durationIn = isMaximized || boundsOffsetY != 0f),
    label = "bounds-rect",
  )

  LaunchedEffect(winAniBounds) {
    pvc.setBounds(winAniBounds.toPureRect())
  }
}