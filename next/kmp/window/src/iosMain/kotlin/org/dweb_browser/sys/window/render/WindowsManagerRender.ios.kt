package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.compose.CompositionChain
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.toOffset
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowRenderConfig
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowsManagerState.Companion.windowImeOutsetBounds
import org.dweb_browser.sys.window.core.constant.debugWindow


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
  params: Map<String, Any?>,
  private val win: WindowController,
  private val windowsManager: WindowsManager<*>,
) {
  val pvc = PureViewController(params, false).also { pvc ->
    pvc.onCreate { params ->
      @Suppress("UNCHECKED_CAST") pvc.addContent {
        val compositionChain by params["compositionChain"] as State<CompositionChain>
        (compositionChain + LocalCompositionChain.current).Provider(LocalWindowsManager provides windowsManager) {
          /// 渲染窗口
          win.WithMaterialTheme {
            win.WindowRender(Modifier.windowImeOutsetBounds())
          }
        }
      }
    }
  }

  companion object {
    private val INSTANCES = WeakHashMap<WindowController, IosWindowNativeView>()
    fun from(
      win: WindowController,
      windowsManager: WindowsManager<*>,
      compositionChain: State<CompositionChain>,
    ) = IosWindowNativeView.INSTANCES.getOrPut(win) {
      IosWindowNativeView(
        mutableMapOf(
          "compositionChain" to compositionChain,
        ), win, windowsManager
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
    val pvc = IosWindowNativeView.from(
      win = win,
      windowsManager = windowsManager,
      compositionChain = rememberUpdatedState(LocalCompositionChain.current)
    ).pvc
    val zIndex by win.watchedState(zIndexBase) { zIndex + zIndexBase }
    val winBounds by win.watchedBounds()
    /// 视图的显示与关闭
    DisposableEffect(pvc) {
      pvc.lifecycleScope.launch {
        nativeViewController.addOrUpdate(pvc, zIndex)
      }
      onDispose {
        pvc.lifecycleScope.launch {
          nativeViewController.remove(pvc)
        }
      }
    }
    /// 视图的一些初始化配置
    LaunchedEffect(pvc) {
      // 我们使用原生的窗口图层
      win.state.renderConfig.isSystemWindow = true
      // 但仍然使用compose进行窗口边框绘制
      win.state.renderConfig.isWindowUseComposeStyle = true
      // 配置圆角
      pvc.uiViewControllerInMain.view.layer.cornerRadius
    }
    /// 绑定窗口的坐标与大小
    val inMove by win.inMove
    if (!inMove) { // 移动中，由移动器自己更新bounds，否则会有延迟
      LaunchedEffect(winBounds) {
        pvc.setBounds(winBounds)
      }
    }
    /// 响应窗口的拖动行为
    val safeBounds = win.safeBounds(LocalWindowLimits.current)
    val density = LocalDensity.current.density
    LaunchedEffect(win) {
      val rootView = pvc.uiViewControllerInMain.view
      var previousPosition = Offset.Zero
      win.state.renderConfig.frameDragDelegate =
        WindowRenderConfig.FrameDragDelegate(
          onStart = { pointerPositionPx ->
            val viewPosition = pvc.getPosition().toOffset()
            val pointerPosition = pointerPositionPx / density
            val screenPosition = pointerPosition + viewPosition
            previousPosition = screenPosition
            println("QAQ onDragStart screenPosition=$screenPosition(pointer=$pointerPosition,view=$viewPosition)")
          },
          onMove = { change, _ ->
            val viewPosition = pvc.getPosition().toOffset()
            val pointerPosition = change.position / density
            val screenPosition = pointerPosition + viewPosition
            val dragAmount = screenPosition - previousPosition
            previousPosition = screenPosition
            val movingWinBounds = win.state.updateBounds {
              moveWindowBoundsInSafeBounds(this, safeBounds, dragAmount)
            }
            pvc.setBounds(movingWinBounds, rootView)
          },
        )
    }

    /// 切换zIndex
    LaunchedEffect(zIndex) {
      nativeViewController.addOrUpdate(pvc, zIndex)
    }
  }
}
